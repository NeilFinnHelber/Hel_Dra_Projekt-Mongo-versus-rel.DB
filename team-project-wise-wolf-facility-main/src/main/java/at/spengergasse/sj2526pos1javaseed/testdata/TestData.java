package at.spengergasse.sj2526pos1javaseed.testdata;

import at.spengergasse.sj2526pos1javaseed.Domain.Genre;
import at.spengergasse.sj2526pos1javaseed.Domain.Language;
import at.spengergasse.sj2526pos1javaseed.Domain.Permission;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * neutrales datenmodell vom testdaten generator
 *
 * - weder jpa entity noch mongo dokument, nur plain records
 * - einzige quelle der wahrheit: die selben objekte gehen an
 *   RelationalTestDataWriter (normalisiert) und
 *   MongoTestDataWriter (CustomerDocument, embedded)
 * - alle ids sind vergeben, alle referenzen zeigen auf
 *   objekte die der generator auch erzeugt hat
 */
public final class TestData {

    private TestData() {
    }

    /** film zeile (+ film_genres, film_languages) */
    public record Film(long id, String title, int releaseYear, float length,
                       List<Language> languages, List<Genre> genres) {
    }

    /** series zeile (+ series_genres, series_languages) */
    public record Series(long id, String title, int releaseYear, int episodes, float length,
                         List<Language> languages, List<Genre> genres) {
    }

    /** store zeile, filmIds/seriesIds = m:n store_films/store_series */
    public record Store(long id, String phoneNumber, List<Long> filmIds, List<Long> seriesIds) {
    }

    /** katalog: einmal pro lauf generiert, klein im vergleich zu den kunden */
    public record Catalog(List<Film> films, List<Series> series, List<Store> stores) {
    }

    /** kunde inkl. aller abhängigen datensätze (aggregate wie im mongo dokument) */
    public record Customer(long id, String nickname, String email,
                           String firstName, String lastName, Permission permission,
                           String street, String city, String zipCode, String country,
                           List<Billing> billings, List<Placement> placements) {
    }

    public record Billing(long id, String cardNumber, LocalDate expirationDate, int cvv) {
    }

    /** order_placement, order ist 1:1 (order_placement.order_id ist unique) */
    public record Placement(long id, LocalDate orderDate, LocalDate orderReturnDate,
                            LocalDate actualReturnDate, Order order) {
    }

    public record Order(long id, LocalDateTime orderDate, BigDecimal totalAmount, long storeId) {
    }
}
