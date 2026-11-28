package at.spengergasse.sj2526pos1javaseed.benchmark.write;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * statistik pro (operation, datenbank, skalierung), nur gemessene läufe (ohne warmups)
 *
 * stddevMs = stichproben standardabweichung (n-1), bei n=1 -> 0
 * recordsPerSecond = records / mittelwert
 */
public record BenchmarkSummary(String operation, String database, int scale, int n,
                               double meanMs, double medianMs, double minMs, double maxMs, double stddevMs,
                               long records, double recordsPerSecond) {

    /** gruppiert in der reihenfolge der ersten messung jeder gruppe */
    public static List<BenchmarkSummary> of(List<BenchmarkMeasurement> measurements) {
        Map<String, List<BenchmarkMeasurement>> groups = new LinkedHashMap<>();
        for (BenchmarkMeasurement m : measurements) {
            if (!m.warmup()) {
                groups.computeIfAbsent(m.operation() + "|" + m.database() + "|" + m.scale(),
                        k -> new ArrayList<>()).add(m);
            }
        }
        List<BenchmarkSummary> out = new ArrayList<>(groups.size());
        for (List<BenchmarkMeasurement> group : groups.values()) {
            BenchmarkMeasurement first = group.getFirst();
            double[] ms = group.stream().mapToDouble(BenchmarkMeasurement::durationMs).sorted().toArray();
            int n = ms.length;
            double mean = 0;
            for (double v : ms) {
                mean += v;
            }
            mean /= n;
            double median = n % 2 == 1 ? ms[n / 2] : (ms[n / 2 - 1] + ms[n / 2]) / 2;
            double sq = 0;
            for (double v : ms) {
                sq += (v - mean) * (v - mean);
            }
            double stddev = n > 1 ? Math.sqrt(sq / (n - 1)) : 0;
            out.add(new BenchmarkSummary(first.operation(), first.database(), first.scale(), n,
                    mean, median, ms[0], ms[n - 1], stddev, first.records(),
                    mean > 0 ? first.records() / (mean / 1000.0) : 0));
        }
        return out;
    }
}
