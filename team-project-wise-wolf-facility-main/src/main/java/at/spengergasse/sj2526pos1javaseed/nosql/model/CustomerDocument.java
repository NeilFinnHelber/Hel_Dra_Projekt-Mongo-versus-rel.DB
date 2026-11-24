package at.spengergasse.sj2526pos1javaseed.nosql.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * json modell für die mongo variante
 *
 * - "optimiert auf frontend" also alles embedded in einem doc
 * - pro kunde genau ein dokument mit adresse + billing + placements
 * - kein nachladen von mehreren collections wie bei relational
 * - bsp dokument: src/main/resources/nosql/customer-document.example.json
 * - modell doku: docs/mongo-dokumentmodell.md
 *
 * extrem embedded: in jedem placement steckt der komplette store
 * inkl. film + series katalog (m:n store_films/store_series
 * als sub dokumente) - absichtlich unnormalisiert für den vergleich
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

    /**
     * verleih vorgang embedded
     * order + store komplett als sub dokumente, keine nackten ids
     */
    public record PlacementJson(OrderJson order,
                                LocalDate orderDate,
                                LocalDate orderReturnDate,
                                LocalDate actualReturnDate) {
    }

    /** order embedded inkl. store + m:n katalog */
    public record OrderJson(Long orderId,
                            LocalDateTime orderDate,
                            BigDecimal totalAmount,
                            StoreJson store) {
    }

    /** store embedded inkl. m:n katalog (store_films, store_series) */
    public record StoreJson(Long storeId,
                            String phoneNumber,
                            List<FilmJson> films,
                            List<SeriesJson> series) {
    }

    /** film embedded (statt film_* spalten + join tabellen) */
    public record FilmJson(Long filmId,
                           String title,
                           Integer releaseYear,
                           Float length,
                           List<String> languages,
                           List<String> genres) {
    }

    /** series embedded (statt series_* spalten + join tabellen) */
    public record SeriesJson(Long seriesId,
                             String title,
                             Integer releaseYear,
                             Integer episodes,
                             Float length,
                             List<String> languages,
                             List<String> genres) {
    }
}
