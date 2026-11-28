package at.spengergasse.sj2526pos1javaseed.benchmark.write;

import at.spengergasse.sj2526pos1javaseed.testdata.TestDataSizes;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * konfiguration eines insert benchmark laufs = request body von POST /tests/insert
 *
 * - alle felder optional, null -> default (issue #8: 3 dbs, 100/1000/100000,
 *   1 warmup + 5 wiederholungen)
 * - wird im konstruktor validiert, ungültige werte -> IllegalArgumentException
 *   (bevor irgendeine db angefasst wird)
 *
 * @param databases   teilmenge von rel, mongo, mongo-ref (einzeln wählbar)
 * @param scales      anzahl kunden pro datensatz, je 10 - 100.000
 * @param repetitions gemessene wiederholungen pro kombination (fließen in die statistik)
 * @param warmups     aufwärmläufe pro kombination, werden exportiert aber nicht ausgewertet
 * @param seed        seed vom testdaten generator
 * @param chunkSize   kunden pro chunk = pro jdbc batch / insertMany
 */
public record BenchmarkConfig(List<String> databases, List<Integer> scales,
                              Integer repetitions, Integer warmups, Long seed, Integer chunkSize) {

    public static final List<String> SUPPORTED_DATABASES = List.of("rel", "mongo", "mongo-ref");
    public static final List<Integer> DEFAULT_SCALES = List.of(100, 1000, 100_000);

    public BenchmarkConfig {
        databases = databases == null || databases.isEmpty() ? SUPPORTED_DATABASES : databases.stream()
                .filter(Objects::nonNull)
                .map(d -> d.strip().toLowerCase(Locale.ROOT))
                .filter(d -> !d.isEmpty())
                .distinct()
                .toList();
        for (String d : databases) {
            if (!SUPPORTED_DATABASES.contains(d)) {
                throw new IllegalArgumentException("unbekannte datenbank '" + d + "', erlaubt: " + SUPPORTED_DATABASES);
            }
        }
        if (databases.isEmpty()) {
            throw new IllegalArgumentException("mindestens eine datenbank angeben: " + SUPPORTED_DATABASES);
        }
        scales = scales == null || scales.isEmpty() ? DEFAULT_SCALES
                : scales.stream().filter(Objects::nonNull).distinct().toList();
        for (int s : scales) {
            if (s < TestDataSizes.MIN_CUSTOMERS || s > TestDataSizes.MAX_CUSTOMERS) {
                throw new IllegalArgumentException("skalierung muss zwischen " + TestDataSizes.MIN_CUSTOMERS
                        + " und " + TestDataSizes.MAX_CUSTOMERS + " liegen, war " + s);
            }
        }
        repetitions = repetitions == null ? 5 : repetitions;
        if (repetitions < 1 || repetitions > 100) {
            throw new IllegalArgumentException("repetitions muss zwischen 1 und 100 liegen, war " + repetitions);
        }
        warmups = warmups == null ? 1 : warmups;
        if (warmups < 0 || warmups > 10) {
            throw new IllegalArgumentException("warmups muss zwischen 0 und 10 liegen, war " + warmups);
        }
        seed = seed == null ? 42L : seed;
        chunkSize = chunkSize == null ? 1000 : chunkSize;
        if (chunkSize < 1 || chunkSize > 10_000) {
            throw new IllegalArgumentException("chunkSize muss zwischen 1 und 10000 liegen, war " + chunkSize);
        }
    }

    /** alle defaults */
    public static BenchmarkConfig defaults() {
        return new BenchmarkConfig(null, null, null, null, null, null);
    }

    public boolean includesRelational() {
        return databases.contains("rel");
    }

    /** mongo und mongo-ref liegen auf demselben mongo server */
    public boolean includesMongo() {
        return databases.contains("mongo") || databases.contains("mongo-ref");
    }
}
