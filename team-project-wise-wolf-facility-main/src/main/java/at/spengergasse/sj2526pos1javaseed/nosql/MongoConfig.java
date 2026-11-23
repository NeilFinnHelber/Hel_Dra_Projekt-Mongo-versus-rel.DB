package at.spengergasse.sj2526pos1javaseed.nosql;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * mongo config
 *
 * - MongoClient + MongoDatabase als springs beans
 * - uri + db name stehen in application.properties
 *   (app.mongodb.uri / app.mongodb.database)
 * - hier werden die sachen nur verdrahtet, nichts weiter
 */
@Configuration
public class MongoConfig {

    @Bean
    public MongoClient mongoClient(@Value("${app.mongodb.uri}") String uri) {
        return MongoClients.create(
                MongoClientSettings.builder()
                        .applyConnectionString(new ConnectionString(uri))
                        .build());
    }

    @Bean
    public MongoDatabase mongoDatabase(MongoClient mongoClient,
                                       @Value("${app.mongodb.database}") String databaseName) {
        return mongoClient.getDatabase(databaseName);
    }
}
