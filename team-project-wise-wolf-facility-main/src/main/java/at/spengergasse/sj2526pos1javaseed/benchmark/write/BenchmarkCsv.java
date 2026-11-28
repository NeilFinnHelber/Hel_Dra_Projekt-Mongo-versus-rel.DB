package at.spengergasse.sj2526pos1javaseed.benchmark.write;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * csv format der insert ergebnisse (auswertungsformat aus #6/#7)
 *
 * <pre>
 * measurements  alle einzelmessungen; spalten wie die k6 csv
 *               (id,database,scale,indexVariant,status,operation,durationMs)
 *               + repetition,warmup,records hinten angehängt
 * summary       statistik pro (operation, db, skalierung)
 * </pre>
 *
 * wird von GET /tests/insert/{id}/*.csv ausgeliefert, k6 speichert es als datei.
 * zahlen immer mit punkt als dezimaltrenner (Locale.ROOT)
 */
public final class BenchmarkCsv {

    static final String MEASUREMENT_HEADER =
            "id,database,scale,indexVariant,status,operation,durationMs,repetition,warmup,records";
    static final String SUMMARY_HEADER =
            "operation,database,scale,n,meanMs,medianMs,minMs,maxMs,stddevMs,records,recordsPerSecond";

    private BenchmarkCsv() {
    }

    public static String measurements(BenchmarkReport report) {
        return String.join("\n", measurementLines(report)) + "\n";
    }

    public static String summary(BenchmarkReport report) {
        return String.join("\n", summaryLines(report)) + "\n";
    }

    static List<String> measurementLines(BenchmarkReport report) {
        List<String> lines = new ArrayList<>();
        lines.add(MEASUREMENT_HEADER);
        for (BenchmarkMeasurement m : report.getMeasurements()) {
            lines.add(String.join(",",
                    String.valueOf(m.id()), m.database(), String.valueOf(m.scale()),
                    "false", "FINISHED", m.operation(), num(m.durationMs()),
                    String.valueOf(m.repetition()), String.valueOf(m.warmup()), String.valueOf(m.records())));
        }
        return lines;
    }

    static List<String> summaryLines(BenchmarkReport report) {
        List<String> lines = new ArrayList<>();
        lines.add(SUMMARY_HEADER);
        for (BenchmarkSummary s : report.getSummaries()) {
            lines.add(String.join(",",
                    s.operation(), s.database(), String.valueOf(s.scale()), String.valueOf(s.n()),
                    num(s.meanMs()), num(s.medianMs()), num(s.minMs()), num(s.maxMs()), num(s.stddevMs()),
                    String.valueOf(s.records()), num(s.recordsPerSecond())));
        }
        return lines;
    }

    private static String num(double v) {
        return String.format(Locale.ROOT, "%.3f", v);
    }
}
