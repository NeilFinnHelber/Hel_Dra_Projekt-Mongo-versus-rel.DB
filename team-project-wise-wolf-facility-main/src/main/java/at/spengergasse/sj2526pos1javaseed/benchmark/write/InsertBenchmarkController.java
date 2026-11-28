package at.spengergasse.sj2526pos1javaseed.benchmark.write;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * http einstieg für den insert benchmark, aufgerufen von k6 (MODE=insert)
 *
 * <pre>
 * POST /tests/insert                     startet async (202), body = BenchmarkConfig, alle felder optional
 * GET  /tests/insert/{id}                report (status, fortschritt, messungen, statistik) als json
 * GET  /tests/insert/{id}/measurements.csv  alle einzelmessungen
 * GET  /tests/insert/{id}/summary.csv       statistik
 * </pre>
 *
 * LEERT die gewählten dbs vor jeder wiederholung, deshalb nur mit
 * app.testdata.endpoint-enabled=true vorhanden (gleicher schalter wie /tests/testdata,
 * default false). die host allowlist gilt zusätzlich.
 */
@RestController
@RequestMapping("/tests/insert")
@ConditionalOnProperty(name = "app.testdata.endpoint-enabled", havingValue = "true")
public class InsertBenchmarkController {

    private static final String CSV = "text/csv;charset=UTF-8";

    private final InsertBenchmark benchmark;

    public InsertBenchmarkController(InsertBenchmark benchmark) {
        this.benchmark = benchmark;
    }

    @PostMapping
    public ResponseEntity<BenchmarkReport> start(@RequestBody(required = false) BenchmarkConfig config) {
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(benchmark.start(config != null ? config : BenchmarkConfig.defaults()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BenchmarkReport> get(@PathVariable long id) {
        return ResponseEntity.of(benchmark.get(id));
    }

    @GetMapping(value = "/{id}/measurements.csv", produces = CSV)
    public ResponseEntity<String> measurementsCsv(@PathVariable long id) {
        return ResponseEntity.of(benchmark.get(id).map(BenchmarkCsv::measurements));
    }

    @GetMapping(value = "/{id}/summary.csv", produces = CSV)
    public ResponseEntity<String> summaryCsv(@PathVariable long id) {
        return ResponseEntity.of(benchmark.get(id).map(BenchmarkCsv::summary));
    }

    /** ungültige config (BenchmarkConfig validiert im konstruktor) -> 400, es wird nichts gelöscht */
    @ExceptionHandler({IllegalArgumentException.class, HttpMessageNotReadableException.class})
    public ResponseEntity<Map<String, String>> badRequest(Exception e) {
        Throwable cause = e;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        return ResponseEntity.badRequest().body(Map.of("error", String.valueOf(cause.getMessage())));
    }
}
