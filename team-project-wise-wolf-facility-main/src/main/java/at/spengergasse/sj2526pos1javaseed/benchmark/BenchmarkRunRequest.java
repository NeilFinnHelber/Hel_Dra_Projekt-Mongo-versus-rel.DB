package at.spengergasse.sj2526pos1javaseed.benchmark;

/**
 * body für POST /tests/run
 *
 * - database: "rel"/"jpa" oder "mongo" (alles andere -> rel)
 * - scale: anzahl customer docs, default 1000
 * - index: true = mit index variante, false = ohne
 */
public record BenchmarkRunRequest(String database, Integer scale, Boolean index) {
}
