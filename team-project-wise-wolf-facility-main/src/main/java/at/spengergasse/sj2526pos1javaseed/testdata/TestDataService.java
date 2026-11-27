package at.spengergasse.sj2526pos1javaseed.testdata;

import at.spengergasse.sj2526pos1javaseed.nosql.MongoCustomerRepository;
import com.mongodb.ConnectionString;
import com.mongodb.client.MongoDatabase;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * eigentliche testdaten generierung, unabhängig vom app start
 *
 * aufgerufen von:
 * - TestDataRunner (cli modus, app.testdata.enabled=true, app beendet sich danach)
 * - TestDataController (POST /tests/testdata in einer laufenden app,
 *   nur mit app.testdata.endpoint-enabled=true)
 *
 * ablauf:
 * 1. parameter prüfen (count 10-100000, target, chunk size)
 * 2. sicherheits check: ziel db hosts müssen in app.testdata.allowed-hosts sein
 *    (default nur lokale/docker hosts) sonst abbruch, bevor irgendwas gelöscht wird
 * 3. ziel db(s) leeren (log welche)
 * 4. katalog einmal generieren und schreiben
 * 5. kunden in chunks generieren, JEDER chunk wird genau einmal erzeugt und
 *    dieselben objekte gehen an beide writer (target=both)
 * 6. zusammenfassung loggen + als Result zurückgeben
 *
 * synchronized: es läuft immer nur eine generierung gleichzeitig
 */
@Slf4j
@Service
public class TestDataService {

    private static final Pattern JDBC_HOSTS = Pattern.compile("^jdbc:postgresql://([^/?]+)");

    /**
     * ergebnis eines laufs, alle zeiten in ms und OHNE app start
     *
     * @param wipeMs       leeren der ziel db(s)
     * @param generationMs nur daten erzeugen im speicher (für beide dbs gemeinsam)
     * @param postgresMs   reine insert zeit postgres (katalog + kunden), 0 wenn nicht ziel
     * @param mongoMs      reine insert zeit mongo (mapping + insertBatch), 0 wenn nicht ziel
     * @param totalMs      generierung + inserts (ohne wipe)
     */
    public record Result(String target, long seed, int chunkSize, TestDataSizes sizes,
                         long wipeMs, long generationMs, long postgresMs, long mongoMs, long totalMs,
                         Map<String, Long> postgresRows, String mongoSummary) {
    }

    private final JdbcTemplate jdbcTemplate;
    private final PlatformTransactionManager transactionManager;
    private final MongoDatabase mongoDatabase;
    private final MongoCustomerRepository mongoCustomerRepository;

    @Value("${app.testdata.allowed-hosts:localhost,127.0.0.1,::1,postgres,mongo,host.docker.internal}")
    private String allowedHosts;

    @Value("${spring.datasource.url:}")
    private String datasourceUrl;

    @Value("${app.mongodb.uri:}")
    private String mongoUri;

    @Value("${app.mongodb.database:}")
    private String mongoDatabaseName;

    public TestDataService(JdbcTemplate jdbcTemplate,
                           PlatformTransactionManager transactionManager,
                           MongoDatabase mongoDatabase,
                           MongoCustomerRepository mongoCustomerRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.transactionManager = transactionManager;
        this.mongoDatabase = mongoDatabase;
        this.mongoCustomerRepository = mongoCustomerRepository;
    }

    public synchronized Result generate(int count, long seed, String target, int chunkSize) {
        String normalizedTarget = normalizeTarget(target);
        boolean rel = "both".equals(normalizedTarget) || "rel".equals(normalizedTarget);
        boolean mongo = "both".equals(normalizedTarget) || "mongo".equals(normalizedTarget);
        if (chunkSize < 1 || chunkSize > 10_000) {
            throw new IllegalArgumentException("app.testdata.chunk-size muss zwischen 1 und 10000 liegen, war " + chunkSize);
        }

        // wirft bei count außerhalb 10-100000, bevor irgendwas gelöscht wird
        TestDataGenerator generator = new TestDataGenerator(seed, count);
        TestDataSizes sizes = generator.sizes();

        Set<String> allowed = Arrays.stream(allowedHosts.split(","))
                .map(s -> s.strip().toLowerCase(Locale.ROOT))
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
        if (rel) {
            assertAllowed("PostgreSQL", postgresHosts(datasourceUrl), allowed);
        }
        if (mongo) {
            assertAllowed("MongoDB", mongoHosts(mongoUri), allowed);
        }

        log.info("Testdaten generator: target={}, seed={}, chunkSize={}", normalizedTarget, seed, chunkSize);
        log.info("Mengengeruest (primaer = customers): customers={}, films={}, series={}, stores={}, "
                        + "films/store={}, series/store={}",
                sizes.customers(), sizes.films(), sizes.series(), sizes.stores(),
                sizes.filmsPerStore(), sizes.seriesPerStore());

        RelationalTestDataWriter relWriter = rel
                ? new RelationalTestDataWriter(jdbcTemplate, new TransactionTemplate(transactionManager))
                : null;
        MongoTestDataWriter mongoWriter = mongo
                ? new MongoTestDataWriter(mongoDatabase, mongoCustomerRepository)
                : null;

        long w = System.nanoTime();
        if (relWriter != null) {
            log.warn("Leere PostgreSQL {} (tabellen: {})", datasourceUrl, String.join(", ", RelationalTestDataWriter.TABLES));
            relWriter.wipe();
        }
        if (mongoWriter != null) {
            log.warn("Leere MongoDB {} / {}.{} (collection wird gedroppt)",
                    redact(mongoUri), mongoDatabaseName, MongoCustomerRepository.COLLECTION_NAME);
            mongoWriter.wipe();
        }
        long wipeNs = System.nanoTime() - w;

        long genNs = 0, relNs = 0, mongoNs = 0;
        long t0 = System.nanoTime();

        long s = System.nanoTime();
        TestData.Catalog catalog = generator.catalog();
        genNs += System.nanoTime() - s;
        if (relWriter != null) {
            s = System.nanoTime();
            relWriter.writeCatalog(catalog);
            relNs += System.nanoTime() - s;
        }
        if (mongoWriter != null) {
            s = System.nanoTime();
            mongoWriter.prepareCatalog(catalog);
            mongoNs += System.nanoTime() - s;
        }
        log.info("Katalog geschrieben: {} films, {} series, {} stores", catalog.films().size(),
                catalog.series().size(), catalog.stores().size());

        int totalChunks = (sizes.customers() + chunkSize - 1) / chunkSize;
        int logEvery = Math.max(1, totalChunks / 20);
        for (int chunk = 0, from = 0; from < sizes.customers(); chunk++, from += chunkSize) {
            int to = Math.min(from + chunkSize, sizes.customers());

            s = System.nanoTime();
            List<TestData.Customer> customers = generator.customers(from, to);
            genNs += System.nanoTime() - s;

            // dieselben objekte für beide dbs
            if (relWriter != null) {
                s = System.nanoTime();
                relWriter.writeCustomers(customers);
                relNs += System.nanoTime() - s;
            }
            if (mongoWriter != null) {
                s = System.nanoTime();
                mongoWriter.writeCustomers(customers);
                mongoNs += System.nanoTime() - s;
            }

            if ((chunk + 1) % logEvery == 0 || to == sizes.customers()) {
                log.info("customers {}/{} ({}%) nach {} s", to, sizes.customers(),
                        to * 100L / sizes.customers(), (System.nanoTime() - t0) / 1_000_000_000.0);
            }
        }
        long totalNs = System.nanoTime() - t0;

        log.info("Fertig in {} ms (wipe {} ms, generierung {} ms, postgres {} ms, mongo {} ms)",
                totalNs / 1_000_000, wipeNs / 1_000_000, genNs / 1_000_000, relNs / 1_000_000, mongoNs / 1_000_000);
        Map<String, Long> rows = relWriter != null ? relWriter.rowCounts() : Map.of();
        String mongoSummary = mongoWriter != null ? mongoWriter.summary() : "";
        if (relWriter != null) {
            log.info("PostgreSQL zeilen: {}", rows);
        }
        if (mongoWriter != null) {
            log.info("MongoDB: {}", mongoSummary);
        }

        return new Result(normalizedTarget, seed, chunkSize, sizes,
                wipeNs / 1_000_000, genNs / 1_000_000, relNs / 1_000_000, mongoNs / 1_000_000, totalNs / 1_000_000,
                rows, mongoSummary);
    }

    /** ergebnis von wipe(), zeiten in ms */
    public record WipeResult(String target, long postgresMs, long mongoMs, long totalMs,
                             Map<String, Long> postgresRows, String mongoSummary) {
    }

    /**
     * nur leeren, ohne neu zu befüllen (TRUNCATE der generator tabellen bzw. drop der collection)
     * gleiche sicherheits checks wie generate()
     */
    public synchronized WipeResult wipe(String target) {
        String normalizedTarget = normalizeTarget(target);
        boolean rel = "both".equals(normalizedTarget) || "rel".equals(normalizedTarget);
        boolean mongo = "both".equals(normalizedTarget) || "mongo".equals(normalizedTarget);

        Set<String> allowed = Arrays.stream(allowedHosts.split(","))
                .map(s -> s.strip().toLowerCase(Locale.ROOT))
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
        if (rel) {
            assertAllowed("PostgreSQL", postgresHosts(datasourceUrl), allowed);
        }
        if (mongo) {
            assertAllowed("MongoDB", mongoHosts(mongoUri), allowed);
        }

        long relNs = 0, mongoNs = 0;
        Map<String, Long> rows = Map.of();
        String mongoSummary = "";
        if (rel) {
            RelationalTestDataWriter relWriter =
                    new RelationalTestDataWriter(jdbcTemplate, new TransactionTemplate(transactionManager));
            log.warn("Leere PostgreSQL {} (tabellen: {})", datasourceUrl, String.join(", ", RelationalTestDataWriter.TABLES));
            long s = System.nanoTime();
            relWriter.wipe();
            relNs = System.nanoTime() - s;
            rows = relWriter.rowCounts();
        }
        if (mongo) {
            MongoTestDataWriter mongoWriter = new MongoTestDataWriter(mongoDatabase, mongoCustomerRepository);
            log.warn("Leere MongoDB {} / {}.{} (collection wird gedroppt)",
                    redact(mongoUri), mongoDatabaseName, MongoCustomerRepository.COLLECTION_NAME);
            long s = System.nanoTime();
            mongoWriter.wipe();
            mongoNs = System.nanoTime() - s;
            mongoSummary = mongoWriter.summary();
        }
        // nur die reine lösch zeit, ohne die zähl abfragen danach
        long totalNs = relNs + mongoNs;
        log.info("Geleert in {} ms (postgres {} ms, mongo {} ms)", totalNs / 1_000_000, relNs / 1_000_000, mongoNs / 1_000_000);
        return new WipeResult(normalizedTarget, relNs / 1_000_000, mongoNs / 1_000_000, totalNs / 1_000_000,
                rows, mongoSummary);
    }

    private static String normalizeTarget(String target) {
        String t = target == null ? "" : target.strip().toLowerCase(Locale.ROOT);
        if (!Set.of("rel", "mongo", "both").contains(t)) {
            throw new IllegalArgumentException("app.testdata.target muss rel, mongo oder both sein, war '" + target + "'");
        }
        return t;
    }

    private static void assertAllowed(String name, List<String> hosts, Set<String> allowed) {
        if (hosts.isEmpty()) {
            throw new IllegalStateException(name + " host konnte nicht ermittelt werden, abbruch");
        }
        for (String host : hosts) {
            if (!allowed.contains(host)) {
                throw new IllegalStateException(name + " host '" + host + "' ist nicht in app.testdata.allowed-hosts "
                        + allowed + " - abbruch, es wurde nichts geloescht");
            }
        }
    }

    static List<String> postgresHosts(String url) {
        Matcher m = url == null ? null : JDBC_HOSTS.matcher(url);
        if (m == null || !m.find()) {
            return List.of();
        }
        return Arrays.stream(m.group(1).split(","))
                .map(TestDataService::stripPort)
                .toList();
    }

    static List<String> mongoHosts(String uri) {
        if (uri == null || uri.isBlank()) {
            return List.of();
        }
        return new ConnectionString(uri).getHosts().stream()
                .map(TestDataService::stripPort)
                .toList();
    }

    private static String stripPort(String hostAndPort) {
        String h = hostAndPort.strip().toLowerCase(Locale.ROOT);
        if (h.startsWith("[")) { // ipv6 [::1]:5432
            int end = h.indexOf(']');
            return end > 0 ? h.substring(1, end) : h;
        }
        int colon = h.lastIndexOf(':');
        return colon > 0 && h.indexOf(':') == colon ? h.substring(0, colon) : h;
    }

    /** zugangsdaten nicht ins log schreiben */
    private static String redact(String uri) {
        return uri == null ? "" : uri.replaceAll("//[^@/]+@", "//***@");
    }
}
