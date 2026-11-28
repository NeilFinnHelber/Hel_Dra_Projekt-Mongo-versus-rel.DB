package at.spengergasse.sj2526pos1javaseed.benchmark.write;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * status + ergebnis eines insert benchmark laufs
 *
 * - wird von POST /tests/insert angelegt (status RUNNING), k6 pollt
 *   GET /tests/insert/{id} bis FINISHED/FAILED (wie BenchmarkResult bei /tests/run)
 * - enthält alle einzelmessungen, statistik und umgebungsinfos
 *   (db versionen, durability)
 */
public class BenchmarkReport {

    public enum Status { RUNNING, FINISHED, FAILED }

    private final long id;
    private final BenchmarkConfig config;
    private final Instant startedAt = Instant.now();
    private final int totalSteps;
    private final List<BenchmarkMeasurement> measurements = new CopyOnWriteArrayList<>();

    private volatile Status status = Status.RUNNING;
    private volatile Instant finishedAt;
    private volatile String error;
    private volatile Map<String, String> environment = Map.of();
    private volatile List<BenchmarkSummary> summaries = List.of();

    public BenchmarkReport(long id, BenchmarkConfig config) {
        this.id = id;
        this.config = config;
        this.totalSteps = config.scales().size() * config.databases().size()
                * (config.warmups() + config.repetitions());
    }

    public long getId() { return id; }
    public String getOperation() { return InsertBenchmark.OPERATION; }
    public BenchmarkConfig getConfig() { return config; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getFinishedAt() { return finishedAt; }
    public Status getStatus() { return status; }
    public String getError() { return error; }
    /** fortschritt für das k6 log beim pollen */
    public int getTotalSteps() { return totalSteps; }
    public int getCompletedSteps() { return measurements.size(); }
    public Map<String, String> getEnvironment() { return environment; }
    public List<BenchmarkMeasurement> getMeasurements() { return List.copyOf(measurements); }
    public List<BenchmarkSummary> getSummaries() { return summaries; }

    void setEnvironment(Map<String, String> environment) { this.environment = Map.copyOf(environment); }

    void add(BenchmarkMeasurement measurement) { measurements.add(measurement); }

    void finish() {
        this.summaries = BenchmarkSummary.of(measurements);
        this.finishedAt = Instant.now();
        this.status = Status.FINISHED;
    }

    void fail(String message) {
        // bis dahin gemessene werte bleiben erhalten + werden ausgewertet
        this.summaries = BenchmarkSummary.of(measurements);
        this.error = message;
        this.finishedAt = Instant.now();
        this.status = Status.FAILED;
    }
}
