package at.spengergasse.sj2526pos1javaseed.presentation;

import at.spengergasse.sj2526pos1javaseed.benchmark.BenchmarkResult;
import at.spengergasse.sj2526pos1javaseed.benchmark.BenchmarkRunRequest;
import at.spengergasse.sj2526pos1javaseed.benchmark.BenchmarkRunner;
import at.spengergasse.sj2526pos1javaseed.benchmark.CompareRunRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * rest api für benchmark testläufe
 *
 * - POST /tests/run startet async (202), langer lauf blockiert nicht
 * - GET /tests/results liefert status + gemessene laufzeiten
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/tests")
@Tag(name = "Benchmark Tests", description = "Start benchmark runs and fetch results")
public class BenchmarkTestController {

    private final BenchmarkRunner benchmarkRunner;

    @Operation(summary = "Run benchmark",
            description = "Start a benchmark run async. Body: {database: rel|mongo, scale: int, index: bool}")
    @PostMapping("/run")
    public ResponseEntity<BenchmarkResult> run(@RequestBody(required = false) BenchmarkRunRequest request) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(benchmarkRunner.startRun(request));
    }

    @Operation(summary = "Compare databases",
            description = "Same dataset into rel + mongo, times the 4 find variants on both "
                    + "and compares result counts. Body: {scales: [1000,10000,100000]} or {scale: n}")
    @PostMapping("/compare")
    public ResponseEntity<List<BenchmarkResult>> compare(@RequestBody(required = false) CompareRunRequest request) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(benchmarkRunner.startCompare(request));
    }

    @Operation(summary = "Get results", description = "All benchmark runs with measured durations")
    @GetMapping("/results")
    public List<BenchmarkResult> results() {
        return benchmarkRunner.results();
    }
}
