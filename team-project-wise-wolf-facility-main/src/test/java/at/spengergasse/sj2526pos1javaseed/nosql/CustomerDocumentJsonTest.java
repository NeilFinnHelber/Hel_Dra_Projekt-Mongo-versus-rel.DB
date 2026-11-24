package at.spengergasse.sj2526pos1javaseed.nosql;

import at.spengergasse.sj2526pos1javaseed.nosql.model.CustomerDocument;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.bson.Document;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * testet serialisierung customerDocument -> json -> bson -> json -> customerDocument
 * ganz ohne laufende mongo instanz
 */
class CustomerDocumentJsonTest {

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .findAndRegisterModules();

    @Test
    void documentSurvivesJsonBsonRoundtrip() throws Exception {
        CustomerDocument doc = new CustomerDocument(
                42L,
                "seed.user.42",
                "seed.user.42@example.com",
                "Anna",
                "Huber42",
                "Customer",
                new CustomerDocument.AddressJson("Seedstraße 1", "Wien", "1010", "Austria"),
                List.of(new CustomerDocument.BillingJson("4000000000000042", "2028-05-01", 777)),
                List.of(new CustomerDocument.PlacementJson(
                        7L,
                        LocalDate.of(2021, 3, 1),
                        LocalDate.of(2021, 3, 15),
                        LocalDate.of(2021, 3, 10),
                        new BigDecimal("19.99"),
                        new CustomerDocument.StoreJson(
                                3L, "+43 1 55500003",
                                List.of(new CustomerDocument.FilmJson(
                                        11L, "Film 11", 2001, 95f,
                                        List.of("German"), List.of("Action"))),
                                List.of(new CustomerDocument.SeriesJson(
                                        21L, "Series 21", 2005, 8, 45f,
                                        List.of("German"), List.of("Drama")))))));

        // pojo -> json -> bson (so wie toDocument)
        Document bson = Document.parse(objectMapper.writeValueAsString(doc));
        assertThat(bson.get("_id")).isEqualTo(42);
        assertThat(bson.getString("nickname")).isEqualTo("seed.user.42");
        assertThat(((Document) bson.get("address")).getString("city")).isEqualTo("Wien");
        assertThat(bson.getList("billingInformation", Document.class)).hasSize(1);
        assertThat(bson.getList("orderPlacements", Document.class)).hasSize(1);

        // bson -> json -> pojo (so wie toDocumentModel)
        CustomerDocument back = objectMapper.readValue(bson.toJson(), CustomerDocument.class);
        assertThat(back).isEqualTo(doc);
    }
}
