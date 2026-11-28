package at.spengergasse.sj2526pos1javaseed.benchmark.write;

import java.time.Instant;

/**
 * eine einzelne messung (eine operation, eine db, eine skalierung, eine wiederholung)
 *
 * - wird unverändert exportiert, damit ausreißer nachvollziehbar bleiben
 * - durationMs mit nachkommastellen (aus nanoTime), bei 100 datensätzen
 *   wären ganze millisekunden zu grob
 *
 * @param id            laufende nummer innerhalb des laufs
 * @param repetition    1..warmups bei aufwärmläufen, 1..repetitions bei gemessenen läufen
 * @param warmup        true = aufwärmlauf, zählt nicht in die statistik
 * @param records       verarbeitete datensätze (kunden = mongo dokumente)
 */
public record BenchmarkMeasurement(long id, String operation, String database, int scale,
                                   int repetition, boolean warmup, long durationNanos, double durationMs,
                                   long records, Instant startedAt) {

    public static BenchmarkMeasurement of(long id, String operation, String database, int scale,
                                          int repetition, boolean warmup, long durationNanos,
                                          long records, Instant startedAt) {
        return new BenchmarkMeasurement(id, operation, database, scale, repetition, warmup,
                durationNanos, durationNanos / 1_000_000.0, records, startedAt);
    }
}
