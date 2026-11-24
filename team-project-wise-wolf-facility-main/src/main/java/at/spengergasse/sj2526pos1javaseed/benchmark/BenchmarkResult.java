package at.spengergasse.sj2526pos1javaseed.benchmark;

import java.math.BigDecimal;
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

    // nur für compare-läufe: art des runs + ergebnisanzahlen beider seiten
    private volatile String type = "run";
    private volatile Map<String, Long> relationalCounts = Map.of();
    private volatile Map<String, Long> mongoCounts = Map.of();
    private volatile Map<String, Long> referencedCounts = Map.of();
    private volatile Map<String, Boolean> variantMatches = Map.of();
    private volatile Boolean countsMatch;

    // aggregations ergebnisse der seiten (stadt -> summe), nur compare
    private volatile Map<String, BigDecimal> relationalAggregation = Map.of();
    private volatile Map<String, BigDecimal> mongoAggregation = Map.of();
    private volatile Map<String, BigDecimal> referencedAggregation = Map.of();

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
    public String getType() { return type; }
    public Map<String, Long> getRelationalCounts() { return relationalCounts; }
    public Map<String, Long> getMongoCounts() { return mongoCounts; }
    public Map<String, Long> getReferencedCounts() { return referencedCounts; }
    public Map<String, Boolean> getVariantMatches() { return variantMatches; }
    public Boolean getCountsMatch() { return countsMatch; }
    public Map<String, BigDecimal> getRelationalAggregation() { return relationalAggregation; }
    public Map<String, BigDecimal> getMongoAggregation() { return mongoAggregation; }
    public Map<String, BigDecimal> getReferencedAggregation() { return referencedAggregation; }

    public void setType(String type) { this.type = type; }

    public void setCompareOutcome(Map<String, Long> relationalCounts,
                                  Map<String, Long> mongoCounts,
                                  Map<String, Long> referencedCounts,
                                  Map<String, Boolean> variantMatches,
                                  boolean countsMatch) {
        this.relationalCounts = relationalCounts;
        this.mongoCounts = mongoCounts;
        this.referencedCounts = referencedCounts;
        this.variantMatches = variantMatches;
        this.countsMatch = countsMatch;
    }

    public void setAggregationOutcome(Map<String, BigDecimal> relational,
                                      Map<String, BigDecimal> mongo,
                                      Map<String, BigDecimal> referenced) {
        this.relationalAggregation = relational;
        this.mongoAggregation = mongo;
        this.referencedAggregation = referenced;
    }

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
