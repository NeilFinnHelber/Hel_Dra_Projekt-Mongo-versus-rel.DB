package at.spengergasse.sj2526pos1javaseed.testdata;

import org.apache.commons.logging.Log;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.boot.logging.DeferredLogFactory;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.Map;

/**
 * schaltet die app in den generator modus wenn app.testdata.enabled=true
 *
 * - app.seed.enabled=false   -> DataInitializer seedet postgres nicht
 *   (DataInitializer selbst bleibt unverändert, liest nur sein flag)
 * - server.port=0 + server.address=127.0.0.1 -> tomcat auf zufälligem
 *   loopback port, kein konflikt mit einer parallel laufenden app auf 8080
 *   und von außen nicht erreichbar (web context selbst wird gebraucht,
 *   die controller hängen an web beans wie PagedResourcesAssembler)
 *
 * höchste priorität, damit z.b. ein env APP_SEED_ENABLED=true den
 * generator modus nicht aushebeln kann
 * registriert in META-INF/spring.factories
 */
public class TestDataModeEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    static final String ENABLED_PROPERTY = "app.testdata.enabled";
    static final String PROPERTY_SOURCE_NAME = "testdataGeneratorMode";

    private final Log log;

    public TestDataModeEnvironmentPostProcessor(DeferredLogFactory logFactory) {
        this.log = logFactory.getLog(TestDataModeEnvironmentPostProcessor.class);
    }

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        if (!environment.getProperty(ENABLED_PROPERTY, Boolean.class, false)) {
            return;
        }
        environment.getPropertySources().addFirst(new MapPropertySource(PROPERTY_SOURCE_NAME, Map.of(
                "app.seed.enabled", "false",
                "server.port", "0",
                "server.address", "127.0.0.1")));
        log.info("Testdaten generator modus aktiv: DataInitializer seed deaktiviert, web server nur auf zufaelligem loopback port");
    }

    @Override
    public int getOrder() {
        // nach dem laden von application.properties / cli args
        return Ordered.LOWEST_PRECEDENCE;
    }
}
