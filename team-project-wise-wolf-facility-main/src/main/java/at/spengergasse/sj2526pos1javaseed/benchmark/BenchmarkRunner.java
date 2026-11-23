package at.spengergasse.sj2526pos1javaseed.benchmark;

import at.spengergasse.sj2526pos1javaseed.benchmark.jpa.JpaPersistenceRepository;
import at.spengergasse.sj2526pos1javaseed.nosql.MongoCustomerRepository;
import at.spengergasse.sj2526pos1javaseed.nosql.model.CustomerDocument;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.Sorts;
import jakarta.annotation.PreDestroy;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

/**
 * testrunner für den mongo vs relational benchmark
 *
 * - POST /tests/run legt ein BenchmarkResult an und startet async
 *   (eigener single thread pool, tests laufen hintereinander,
 *   request läuft nicht in timeout)
 * - GET /tests/results liefert alle läufe
 * - vor jedem lauf werden die benchmark daten zurückgesetzt,
 *   damit die ergebnisse vergleichbar sind
 *
 * Ablauf pro lauf:
 * - index anlegen oder droppen (je nach variante)
 * - scale customer docs generieren (ids ab max(id)+1,
 *   damit wiederholte läufe nicht kollidieren)
 * - alle PersistenceRepository operationen nacheinander timen
 *
 * hinweis: generierte docs nutzen eigene stadt "Benchmarkstadt",
 * damit findByCity + deleteByCity die seed daten nicht mitnehmen
 * (updateLastNameAll trifft trotzdem alles, ist so definiert)
 */
@Service
public class BenchmarkRunner {

    private static final Logger log = LoggerFactory.getLogger(BenchmarkRunner.class);

    private static final String BENCH_CITY = "Benchmarkstadt";
    private static final int FIND_LIMIT = 1000;
    private static final int INSERT_BATCH_SIZE = 500;
    private static final int DEFAULT_SCALE = 1000;
    private static final int MAX_SCALE = 100_000;

    private static final String[] FIRST_NAMES = {"Anna", "Ben", "Clara", "David", "Elena", "Felix", "Greta"};
    private static final String[] LAST_NAMES = {"Huber", "Gruber", "Bauer", "Wagner", "Moser", "Pichler"};
    private static final String[] PERMISSIONS = {"Customer", "Customer", "Customer", "Admin", "Employee"};

    private final JpaPersistenceRepository jpaRepository;
    private final MongoCustomerRepository mongoRepository;
    private final MongoDatabase mongoDatabase;
    private final JdbcTemplate jdbcTemplate;

    // single thread: tests müssen hintereinander laufen (issue vorgabe)
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final List<BenchmarkResult> results = new CopyOnWriteArrayList<>();
    private final AtomicLong idSeq = new AtomicLong();

    public BenchmarkRunner(JpaPersistenceRepository jpaRepository,
                           MongoCustomerRepository mongoRepository,
                           MongoDatabase mongoDatabase,
                           JdbcTemplate jdbcTemplate) {
        this.jpaRepository = jpaRepository;
        this.mongoRepository = mongoRepository;
        this.mongoDatabase = mongoDatabase;
        this.jdbcTemplate = jdbcTemplate;
    }

    /** startet lauf async, gibt result sofort zurück (status RUNNING) */
    public BenchmarkResult startRun(BenchmarkRunRequest request) {
        String database = normalize(request != null ? request.database() : null);
        int scale = request != null && request.scale() != null
                ? Math.min(Math.max(request.scale(), 1), MAX_SCALE)
                : DEFAULT_SCALE;
        boolean index = request != null && Boolean.TRUE.equals(request.index());

        BenchmarkResult result = new BenchmarkResult(idSeq.incrementAndGet(), database, scale, index);
        results.add(result);
        executor.submit(() -> execute(result));
        return result;
    }

    public List<BenchmarkResult> results() {
        return List.copyOf(results);
    }

    @PreDestroy
    void shutdown() {
        executor.shutdownNow();
    }

    // ---------- ausführung ----------

    private void execute(BenchmarkResult result) {
        try {
            PersistenceRepository repo = "mongo".equals(result.getDatabase())
                    ? mongoRepository
                    : jpaRepository;

            // db zurücksetzen vor jedem lauf, sonst sind zeiten nicht vergleichbar
            resetBenchmarkData(result.getDatabase());

            applyIndex(result.getDatabase(), result.isIndexVariant());

            long base = idBase(result.getDatabase());
            List<CustomerDocument> docs = generateDocuments(result.getScale(), base);
            int batchSize = Math.min(INSERT_BATCH_SIZE, result.getScale());
            int limit = Math.min(FIND_LIMIT, result.getScale());

            Map<String, Long> timings = new LinkedHashMap<>();

            timings.put("insertBatchMs", time(() -> repo.insertBatch(docs, batchSize)));

            String firstNickname = "bench.user." + (base + 1);
            timings.put("findAllMs", time(() -> repo.findAll(limit)));
            timings.put("findByCityMs", time(() -> repo.findByCity(BENCH_CITY, limit)));
            timings.put("findByCityProjectionMs", time(() -> repo.findByCityProjection(BENCH_CITY, limit)));
            timings.put("findByCityProjectionSortedMs",
                    time(() -> repo.findByCityProjectionSorted(BENCH_CITY, true, limit)));
            timings.put("updateLastNameByNicknameMs",
                    time(() -> repo.updateLastNameByNickname(firstNickname, "Benchmark")));
            timings.put("updateLastNameAllMs", time(() -> repo.updateLastNameAll("BenchmarkAll")));
            timings.put("deleteByCityMs", time(() -> repo.deleteByCity(BENCH_CITY)));

            result.finish(timings, docs.size());
            log.info("benchmark run {} ({}, scale={}, index={}) finished: {}",
                    result.getId(), result.getDatabase(), result.getScale(),
                    result.isIndexVariant(), timings);
        } catch (Exception e) {
            log.error("benchmark run {} failed", result.getId(), e);
            result.fail(e.getMessage());
        }
    }

    private static long time(Runnable r) {
        long t0 = System.nanoTime();
        r.run();
        return (System.nanoTime() - t0) / 1_000_000;
    }

    private static String normalize(String database) {
        if (database != null && database.toLowerCase().contains("mongo")) {
            return "mongo";
        }
        return "rel";
    }

    // ---------- reset ----------

    /**
     * löscht nur benchmark daten (bench.user.* / Benchmarkstadt),
     * seed daten bleiben unangetastet
     */
    private void resetBenchmarkData(String database) {
        if ("mongo".equals(database)) {
            MongoCollection<Document> collection =
                    mongoDatabase.getCollection(MongoCustomerRepository.COLLECTION_NAME);
            collection.deleteMany(Filters.or(
                    Filters.eq("address.city", BENCH_CITY),
                    Filters.regex("nickname", Pattern.compile("^bench\\.user\\."))));
        } else {
            // fk reihenfolge: kinder vor parent (placements, billings, dann customer)
            jdbcTemplate.update(
                    "delete from order_placement where customer_id in "
                            + "(select id from customer where nickname like 'bench.user.%')");
            jdbcTemplate.update(
                    "delete from billing_information where customer_id in "
                            + "(select id from customer where nickname like 'bench.user.%')");
            jdbcTemplate.update("delete from customer where nickname like 'bench.user.%'");
        }
    }

    // ---------- index varianten ----------

    private void applyIndex(String database, boolean withIndex) {
        if ("mongo".equals(database)) {
            MongoCollection<Document> collection =
                    mongoDatabase.getCollection(MongoCustomerRepository.COLLECTION_NAME);
            if (withIndex) {
                collection.createIndex(Indexes.ascending("address.city"),
                        new IndexOptions().name("bench_city_idx"));
                collection.createIndex(Indexes.ascending("nickname"),
                        new IndexOptions().name("bench_nick_idx"));
            } else {
                dropIndexQuietly(collection, "bench_city_idx");
                dropIndexQuietly(collection, "bench_nick_idx");
            }
        } else {
            if (withIndex) {
                jdbcTemplate.execute(
                        "CREATE INDEX IF NOT EXISTS bench_customer_city_idx ON customer(address_city)");
                jdbcTemplate.execute(
                        "CREATE INDEX IF NOT EXISTS bench_customer_nick_idx ON customer(nickname)");
            } else {
                jdbcTemplate.execute("DROP INDEX IF EXISTS bench_customer_city_idx");
                jdbcTemplate.execute("DROP INDEX IF EXISTS bench_customer_nick_idx");
            }
        }
    }

    private void dropIndexQuietly(MongoCollection<Document> collection, String name) {
        try {
            collection.dropIndex(name);
        } catch (Exception ignored) {
            // index existiert nicht, passt
        }
    }

    // ---------- id basis ----------

    /** höchste vorhandene id, damit neue docs nicht mit bestehenden kollidieren */
    private long idBase(String database) {
        if ("mongo".equals(database)) {
            MongoCollection<Document> collection =
                    mongoDatabase.getCollection(MongoCustomerRepository.COLLECTION_NAME);
            Document last = collection.find()
                    .sort(Sorts.descending("_id"))
                    .limit(1)
                    .first();
            Object id = last != null ? last.get("_id") : null;
            return id instanceof Number n ? n.longValue() : 0L;
        }
        Long max = jdbcTemplate.queryForObject(
                "select coalesce(max(id), 0) from customer", Long.class);
        return max != null ? max : 0L;
    }

    // ---------- doc generierung ----------

    private List<CustomerDocument> generateDocuments(int scale, long idBase) {
        List<CustomerDocument> docs = new ArrayList<>(scale);
        for (int i = 0; i < scale; i++) {
            long id = idBase + i + 1;
            docs.add(new CustomerDocument(
                    id,
                    "bench.user." + id,
                    "bench.user." + id + "@example.com",
                    FIRST_NAMES[i % FIRST_NAMES.length],
                    LAST_NAMES[i % LAST_NAMES.length] + id,
                    PERMISSIONS[i % PERMISSIONS.length],
                    new CustomerDocument.AddressJson(
                            "Benchmarkstraße " + (i % 200 + 1),
                            BENCH_CITY,
                            String.format("%04d", 1000 + i % 9000),
                            "Austria"),
                    List.of(new CustomerDocument.BillingJson(
                            "4" + String.format("%015d", id % 1_000_000_000_000_000L),
                            "2028-01-01",
                            100 + i % 900)),
                    List.of(new CustomerDocument.PlacementJson(
                            id, // orderId = doc id, damit unique (order_placement.order_id ist unique)
                            LocalDate.of(2021, 1, 1).plusDays(i % 1500),
                            LocalDate.of(2021, 1, 15).plusDays(i % 1500),
                            i % 4 == 0 ? null : LocalDate.of(2021, 1, 10).plusDays(i % 1500),
                            BigDecimal.valueOf(5 + (i * 37) % 500).setScale(2, java.math.RoundingMode.HALF_UP),
                            1L))));
        }
        return docs;
    }
}
