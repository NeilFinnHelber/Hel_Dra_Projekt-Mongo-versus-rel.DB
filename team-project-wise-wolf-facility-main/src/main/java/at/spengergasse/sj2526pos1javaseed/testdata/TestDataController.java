package at.spengergasse.sj2526pos1javaseed.testdata;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * testdaten generator in einer bereits laufenden app auslösen, damit die
 * gemessenen zeiten nur generierung + inserts enthalten, nicht den app start
 *
 * POST /tests/testdata  body (alle felder optional, defaults = app.testdata.*):
 *   {"count": 10000, "seed": 42, "target": "both", "chunkSize": 1000}
 * antwort: TestDataService.Result mit zeiten in ms
 *
 * LEERT die ziel db(s)! deshalb nur mit app.testdata.endpoint-enabled=true
 * vorhanden (default false), host allowlist gilt wie im cli modus
 */
@RestController
@RequestMapping("/tests/testdata")
@ConditionalOnProperty(name = "app.testdata.endpoint-enabled", havingValue = "true")
public class TestDataController {

    public record Request(Integer count, Long seed, String target, Integer chunkSize) {
    }

    private final TestDataService service;

    @Value("${app.testdata.count:1000}")
    private int defaultCount;

    @Value("${app.testdata.seed:" + TestDataGenerator.DEFAULT_SEED + "}")
    private long defaultSeed;

    @Value("${app.testdata.target:both}")
    private String defaultTarget;

    @Value("${app.testdata.chunk-size:1000}")
    private int defaultChunkSize;

    public TestDataController(TestDataService service) {
        this.service = service;
    }

    @PostMapping
    public TestDataService.Result run(@RequestBody(required = false) Request request) {
        Request r = request != null ? request : new Request(null, null, null, null);
        return service.generate(
                r.count() != null ? r.count() : defaultCount,
                r.seed() != null ? r.seed() : defaultSeed,
                r.target() != null ? r.target() : defaultTarget,
                r.chunkSize() != null ? r.chunkSize() : defaultChunkSize);
    }

    /** DELETE /tests/testdata?target=both  -> ziel db(s) nur leeren */
    @DeleteMapping
    public TestDataService.WipeResult wipe(@RequestParam(required = false) String target) {
        return service.wipe(target != null ? target : defaultTarget);
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<Map<String, String>> badRequest(RuntimeException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", String.valueOf(e.getMessage())));
    }
}
