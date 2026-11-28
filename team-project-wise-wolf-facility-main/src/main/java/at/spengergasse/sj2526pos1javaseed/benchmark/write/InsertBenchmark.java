package at.spengergasse.sj2526pos1javaseed.benchmark.write;

import at.spengergasse.sj2526pos1javaseed.nosql.MongoCustomerRepository;
import at.spengergasse.sj2526pos1javaseed.nosql.model.CustomerDocument;
import at.spengergasse.sj2526pos1javaseed.nosql.ref.MongoReferencedRepository;
import at.spengergasse.sj2526pos1javaseed.testdata.MongoReferencedTestDataWriter;
import at.spengergasse.sj2526pos1javaseed.testdata.MongoTestDataWriter;
import at.spengergasse.sj2526pos1javaseed.testdata.RelationalTestDataWriter;
import at.spengergasse.sj2526pos1javaseed.testdata.TestData;
import at.spengergasse.sj2526pos1javaseed.testdata.TestDataService;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoDatabase;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

/**
 * insert benchmark (issue #8): kompletten datensatz in eine leere datenbank schreiben,
 * für rel, mongo und mongo-ref, mehrere skalierungen und wiederholungen
 *
 * <pre>
 * host allowlist prüfen (vor jedem löschen)
 * pro skalierung:
 *   datensatz einmal generieren (fixer seed, nicht gemessen)
 *   pro wiederholung (erst warmups, dann gemessene):
 *     pro datenbank (reihenfolge rotiert pro wiederholung):
 *       prepare   nicht gemessen  db leeren
 *       execute   gemessen        datensatz einfügen (System.nanoTime)
 *       verify    nicht gemessen  zeilen/dokumente zählen
 * statistik über die gemessenen wiederholungen
 * </pre>
 *
 * execute pro db:
 *   rel       katalog einmal, dann pro chunk eine transaktion mit jdbc batches
 *             (RelationalTestDataWriter, reWriteBatchedInserts=true)
 *   mongo     pro chunk ein insertMany der embedded CustomerDocuments
 *             (bson konvertierung im MongoCustomerRepository zählt mit)
 *   mongo-ref katalog einmal, dann pro chunk ein insertMany pro ref_* collection
 *             (normalisierung im MongoReferencedRepository zählt mit)
 *
 * - start() läuft async auf einem single thread executor (wie BenchmarkRunner),
 *   ergebnisse nur im speicher, abruf per id
 * - fehler -> report status FAILED, bisherige messungen bleiben erhalten
 */
@Slf4j
@Service
public class InsertBenchmark {

    public static final String OPERATION = "insert";

    private final TestDataService testDataService;
    private final JdbcTemplate jdbcTemplate;
    private final MongoClient mongoClient;
    private final MongoDatabase mongoDatabase;
    private final RelationalTestDataWriter relWriter;
    private final MongoTestDataWriter mongoWriter;
    private final MongoReferencedTestDataWriter refWriter;

    // läufe müssen hintereinander laufen, sonst stören sie sich gegenseitig
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Map<Long, BenchmarkReport> reports = new ConcurrentHashMap<>();
    private final AtomicLong ids = new AtomicLong();

    @Value("${spring.datasource.url:}")
    private String datasourceUrl = "";

    public InsertBenchmark(TestDataService testDataService,
                           JdbcTemplate jdbcTemplate,
                           PlatformTransactionManager transactionManager,
                           MongoClient mongoClient,
                           MongoDatabase mongoDatabase,
                           MongoCustomerRepository mongoCustomerRepository,
                           MongoReferencedRepository mongoReferencedRepository) {
        this.testDataService = testDataService;
        this.jdbcTemplate = jdbcTemplate;
        this.mongoClient = mongoClient;
        this.mongoDatabase = mongoDatabase;
        this.relWriter = new RelationalTestDataWriter(jdbcTemplate, new TransactionTemplate(transactionManager));
        this.mongoWriter = new MongoTestDataWriter(mongoDatabase, mongoCustomerRepository);
        this.refWriter = new MongoReferencedTestDataWriter(mongoDatabase, mongoReferencedRepository);
    }

    /** startet einen lauf async, gibt den report sofort zurück (status RUNNING) */
    public BenchmarkReport start(BenchmarkConfig config) {
        BenchmarkReport report = new BenchmarkReport(ids.incrementAndGet(), config);
        reports.put(report.getId(), report);
        executor.submit(() -> run(report));
        return report;
    }

    public Optional<BenchmarkReport> get(long id) {
        return Optional.ofNullable(reports.get(id));
    }

    @PreDestroy
    void shutdown() {
        executor.shutdownNow();
    }

    /** führt den lauf synchron aus (vom executor bzw. direkt aus den tests) */
    void run(BenchmarkReport report) {
        BenchmarkConfig config = report.getConfig();
        try {
            testDataService.assertTargetsAllowed(config.includesRelational(), config.includesMongo());
            report.setEnvironment(describeEnvironment(config));
            log.info("Insert benchmark {} startet: databases={}, scales={}, warmups={}, repetitions={}, seed={}, chunkSize={}",
                    report.getId(), config.databases(), config.scales(), config.warmups(),
                    config.repetitions(), config.seed(), config.chunkSize());

            long id = 0;
            for (int scale : config.scales()) {
                long g = System.nanoTime();
                BenchmarkDataset dataset = BenchmarkDataset.generate(config.seed(), scale, config.chunkSize());
                log.info("Datensatz scale={} generiert in {} ms (nicht gemessen)", scale,
                        (System.nanoTime() - g) / 1_000_000);

                int total = config.warmups() + config.repetitions();
                for (int run = 0; run < total; run++) {
                    boolean warmup = run < config.warmups();
                    int repetition = warmup ? run + 1 : run - config.warmups() + 1;
                    for (String database : rotate(config.databases(), run)) {
                        prepare(database, dataset);

                        Instant startedAt = Instant.now();
                        long t0 = System.nanoTime();
                        long records = execute(database, dataset);
                        long nanos = System.nanoTime() - t0;

                        verify(database, dataset);

                        BenchmarkMeasurement m = BenchmarkMeasurement.of(++id, OPERATION, database, scale,
                                repetition, warmup, nanos, records, startedAt);
                        report.add(m);
                        log.info("[{}/{}] insert {} scale={} {} {}: {} ms", report.getCompletedSteps(),
                                report.getTotalSteps(), database, scale, warmup ? "warmup" : "rep", repetition,
                                String.format("%.3f", m.durationMs()));
                    }
                }
            }
            report.finish();
        } catch (Exception e) {
            log.error("Insert benchmark {} fehlgeschlagen: {}", report.getId(), e.getMessage(), e);
            report.fail(e.getMessage());
        }
    }

    // ---------- pro datenbank: leeren / einfügen / prüfen ----------

    /** nicht gemessen: db leeren (TRUNCATE bzw. collections droppen + leer neu anlegen) */
    void prepare(String database, BenchmarkDataset dataset) {
        switch (database) {
            case "rel" -> relWriter.wipe();
            case "mongo" -> mongoWriter.wipe();
            case "mongo-ref" -> refWriter.wipe();
            default -> throw new IllegalArgumentException("unbekannte datenbank " + database);
        }
    }

    /** gemessen: nur die persistenz arbeit, @return anzahl kunden */
    long execute(String database, BenchmarkDataset dataset) {
        int chunk = dataset.chunkSize();
        switch (database) {
            case "rel" -> {
                relWriter.writeCatalog(dataset.catalog());
                for (List<TestData.Customer> customers : BenchmarkDataset.chunks(dataset.customers(), chunk)) {
                    relWriter.writeCustomers(customers);
                }
            }
            case "mongo" -> {
                for (List<CustomerDocument> docs : BenchmarkDataset.chunks(dataset.documents(), chunk)) {
                    mongoWriter.writeDocuments(docs);
                }
            }
            case "mongo-ref" -> {
                refWriter.writeCatalog(dataset.stores());
                for (List<CustomerDocument> docs : BenchmarkDataset.chunks(dataset.documents(), chunk)) {
                    refWriter.writeDocuments(docs);
                }
            }
            default -> throw new IllegalArgumentException("unbekannte datenbank " + database);
        }
        return dataset.scale();
    }

    /** nicht gemessen: zeilen/dokument anzahlen gegen den datensatz, sonst IllegalStateException */
    void verify(String database, BenchmarkDataset dataset) {
        switch (database) {
            case "rel" -> assertCounts(database, dataset.expectedRelationalRows(), relWriter.rowCounts());
            case "mongo" -> assertCounts(database,
                    Map.of(MongoCustomerRepository.COLLECTION_NAME, (long) dataset.scale()),
                    Map.of(MongoCustomerRepository.COLLECTION_NAME, mongoWriter.count()));
            case "mongo-ref" -> assertCounts(database, dataset.expectedReferencedCounts(), refWriter.counts());
            default -> throw new IllegalArgumentException("unbekannte datenbank " + database);
        }
    }

    static void assertCounts(String database, Map<String, Long> expected, Map<String, Long> actual) {
        for (Map.Entry<String, Long> e : expected.entrySet()) {
            Long got = actual.get(e.getKey());
            if (!e.getValue().equals(got)) {
                throw new IllegalStateException(database + ": " + e.getKey() + " erwartet " + e.getValue()
                        + " datensätze, gefunden " + got + " (alle: " + actual + ")");
            }
        }
    }

    /** reihenfolge pro wiederholung verschieben, damit keine db immer als erste/letzte läuft */
    static List<String> rotate(List<String> databases, int run) {
        List<String> out = new ArrayList<>(databases.size());
        for (int i = 0; i < databases.size(); i++) {
            out.add(databases.get((i + run) % databases.size()));
        }
        return out;
    }

    // ---------- rahmenbedingungen für den export ----------

    /**
     * db versionen + durability (postgres synchronous_commit, mongo write concern) + insert pfad
     * fehler beim abfragen brechen den lauf nicht ab, der wert wird dann "n/a (...)"
     */
    Map<String, String> describeEnvironment(BenchmarkConfig config) {
        Map<String, String> env = new LinkedHashMap<>();
        env.put("javaVersion", System.getProperty("java.version"));
        if (config.includesRelational()) {
            env.put("postgresVersion", safe(() -> jdbcTemplate.queryForObject("show server_version", String.class)));
            env.put("postgresSynchronousCommit",
                    safe(() -> jdbcTemplate.queryForObject("show synchronous_commit", String.class)));
            env.put("postgresInsertPath", "JDBC batch (RelationalTestDataWriter), 1 transaction per chunk, "
                    + "reWriteBatchedInserts=" + datasourceUrl.contains("reWriteBatchedInserts=true"));
        }
        if (config.includesMongo()) {
            env.put("mongoVersion", safe(() -> admin("buildInfo").getString("version")));
            env.put("mongoTopology", safe(() -> {
                Document hello = admin("hello");
                return hello.containsKey("setName") ? "replicaSet " + hello.getString("setName") : "standalone";
            }));
            // client seitig nicht gesetzt = ACKNOWLEDGED mit w/j = null -> server default gilt
            env.put("mongoWriteConcernClient", safe(() -> mongoDatabase.getWriteConcern().toString()));
            env.put("mongoWriteConcernServerDefault", safe(() -> {
                Document res = admin("getDefaultRWConcern");
                Object wc = res.get("defaultWriteConcern");
                return (wc instanceof Document d ? d.toJson() : String.valueOf(wc))
                        + " (source=" + res.get("defaultWriteConcernSource") + ")";
            }));
            env.put("mongoInsertPath", "insertMany per chunk and collection (mongodb-driver-sync)");
        }
        return env;
    }

    private Document admin(String command) {
        return mongoClient.getDatabase("admin").runCommand(new Document(command, 1));
    }

    private static String safe(ThrowingSupplier supplier) {
        try {
            String value = supplier.get();
            return value != null ? value : "n/a";
        } catch (Exception e) {
            return "n/a (" + e.getMessage() + ")";
        }
    }

    @FunctionalInterface
    private interface ThrowingSupplier {
        String get() throws Exception;
    }
}
