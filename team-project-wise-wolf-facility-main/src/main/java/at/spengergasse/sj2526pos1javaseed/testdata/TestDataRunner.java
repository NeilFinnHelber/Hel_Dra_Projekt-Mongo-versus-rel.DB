package at.spengergasse.sj2526pos1javaseed.testdata;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * cli modus des testdaten generators, NUR mit app.testdata.enabled=true
 *
 * liest die app.testdata.* properties, ruft TestDataService.generate auf und
 * beendet danach die app (exit code 0, fehler = 1)
 *
 * zum messen ohne app start: stattdessen POST /tests/testdata (TestDataController)
 * aufruf siehe docs/testdatengenerator.md
 */
@Slf4j
@Component
@ConditionalOnProperty(name = TestDataModeEnvironmentPostProcessor.ENABLED_PROPERTY, havingValue = "true")
public class TestDataRunner implements ApplicationRunner {

    private final TestDataService service;
    private final ConfigurableApplicationContext context;

    @Value("${app.testdata.count:1000}")
    private int count;

    @Value("${app.testdata.seed:" + TestDataGenerator.DEFAULT_SEED + "}")
    private long seed;

    @Value("${app.testdata.target:both}")
    private String target;

    @Value("${app.testdata.chunk-size:1000}")
    private int chunkSize;

    private volatile int exitCode = 1;

    public TestDataRunner(TestDataService service, ConfigurableApplicationContext context) {
        this.service = service;
        this.context = context;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            service.generate(count, seed, target, chunkSize);
            exitCode = 0;
        } catch (Exception e) {
            log.error("Testdaten generierung fehlgeschlagen: {}", e.getMessage(), e);
            exitCode = 1;
        }
    }

    /** nach allen runnern: prozess sauber beenden statt weiterlaufen */
    @EventListener(ApplicationReadyEvent.class)
    public void exitWhenDone() {
        int code = SpringApplication.exit(context, () -> exitCode);
        System.exit(code);
    }
}
