package at.spengergasse.sj2526pos1javaseed.benchmark;

import java.time.Instant;
import java.util.Map;

/**
 * status + ergebnis eines benchmark laufs
 *
 * - wird von POST /tests/run angelegt (status RUNNING)
 * - runner trägt durationsMs etc nach, wenn fertig
 * - frontend fragt über GET /tests/results ab
 */
public class BenchmarkResult {

    public enum Status { RUNNING, FINISHED, FAILED }

    private final long id;
    private final String database;
    private final int scale;
    private final boolean indexVariant;
    private final Instant startedAt;

    private volatile Status status = Status.RUNNING;
    private volatile Instant finishedAt;
    private volatile String error;
    private volatile long insertedCount;
    private volatile Map<String, Long> durationsMs = Map.of();

    public BenchmarkResult(long id, String database, int scale, boolean indexVariant) {
        this.id = id;
        this.database = database;
        this.scale = scale;
        this.indexVariant = indexVariant;
        this.startedAt = Instant.now();
    }

    public long getId() { return id; }
    public String getDatabase() { return database; }
    public int getScale() { return scale; }
    public boolean isIndexVariant() { return indexVariant; }
    public Instant getStartedAt() { return startedAt; }
    public Status getStatus() { return status; }
    public Instant getFinishedAt() { return finishedAt; }
    public String getError() { return error; }
    public long getInsertedCount() { return insertedCount; }
    public Map<String, Long> getDurationsMs() { return durationsMs; }

    public void finish(Map<String, Long> durations, long insertedCount) {
        this.durationsMs = durations;
        this.insertedCount = insertedCount;
        this.finishedAt = Instant.now();
        this.status = Status.FINISHED;
    }

    public void fail(String message) {
        this.error = message;
        this.finishedAt = Instant.now();
        this.status = Status.FAILED;
    }
}
