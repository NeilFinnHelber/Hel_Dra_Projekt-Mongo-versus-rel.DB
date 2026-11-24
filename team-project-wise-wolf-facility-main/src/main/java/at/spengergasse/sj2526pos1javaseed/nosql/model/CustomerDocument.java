package at.spengergasse.sj2526pos1javaseed.nosql.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * json modell für die mongo variante
 *
 * - "optimiert auf frontend" also alles embedded in einem doc
 * - pro kunde genau ein dokument mit adresse + billing + placements
 * - kein nachladen von mehreren collections wie bei relational
 * - bsp dokument: src/main/resources/nosql/customer-document.example.json
 * - modell doku: docs/mongo-dokumentmodell.md
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CustomerDocument(
        @JsonProperty("_id") Long id,
        String nickname,
        String email,
        String firstName,
        String lastName,
        String permission,
        AddressJson address,
        List<BillingJson> billingInformation,
        List<PlacementJson> orderPlacements) {

    /** adresse embedded, statt address_* spalten */
    public record AddressJson(String street, String city, String zipCode, String country) {
    }

    /** zahlungsdaten des kunden embedded */
    public record BillingJson(String cardNumber, String expirationDate, int cvv) {
    }

    /** verleih vorgänge embedded (order_placement + order daten) */
    public record PlacementJson(Long orderId,
                                LocalDate orderDate,
                                LocalDate orderReturnDate,
                                LocalDate actualReturnDate,
                                BigDecimal totalAmount,
                                Long storeId) {
    }
}
