package at.spengergasse.sj2526pos1javaseed.testdata;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.Date;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * schreibt TestData normalisiert nach postgres (flyway schema)
 *
 * - plain jdbc batch inserts statt jpa: hier wird befüllt, nicht gemessen,
 *   und bei 100k kunden (~ 600k zeilen) ist hibernate overhead unnötig
 * - fk reihenfolge: film/series (+ element collections) -> store
 *   -> store_films/store_series -> customer -> billing -> orders -> placement
 * - pro chunk eine transaktion
 * - keine spring bean: wird vom TestDataService bzw. vom write benchmark gebaut
 */
public class RelationalTestDataWriter {

    /** alle tabellen die der generator befüllt (phone_numbers hat keine fk und kein mongo pendant) */
    public static final List<String> TABLES = List.of(
            "order_placement", "orders", "billing_information", "customer",
            "store_films", "store_series", "store",
            "film_genres", "film_languages", "film",
            "series_genres", "series_languages", "series");

    private final JdbcTemplate jdbc;
    private final TransactionTemplate tx;

    public RelationalTestDataWriter(JdbcTemplate jdbc, TransactionTemplate tx) {
        this.jdbc = jdbc;
        this.tx = tx;
    }

    /** destruktiv: leert alle generator tabellen (flyway_schema_history bleibt) */
    public void wipe() {
        jdbc.execute("TRUNCATE TABLE " + String.join(", ", TABLES));
    }

    public void writeCatalog(TestData.Catalog catalog) {
        List<Object[]> films = new ArrayList<>();
        List<Object[]> filmGenres = new ArrayList<>();
        List<Object[]> filmLanguages = new ArrayList<>();
        for (TestData.Film f : catalog.films()) {
            films.add(new Object[]{f.id(), f.title(), f.releaseYear(), f.length()});
            f.genres().forEach(g -> filmGenres.add(new Object[]{f.id(), g.name()}));
            f.languages().forEach(l -> filmLanguages.add(new Object[]{f.id(), l.name()}));
        }
        List<Object[]> series = new ArrayList<>();
        List<Object[]> seriesGenres = new ArrayList<>();
        List<Object[]> seriesLanguages = new ArrayList<>();
        for (TestData.Series s : catalog.series()) {
            series.add(new Object[]{s.id(), s.title(), s.releaseYear(), s.episodes(), s.length()});
            s.genres().forEach(g -> seriesGenres.add(new Object[]{s.id(), g.name()}));
            s.languages().forEach(l -> seriesLanguages.add(new Object[]{s.id(), l.name()}));
        }
        List<Object[]> stores = new ArrayList<>();
        List<Object[]> storeFilms = new ArrayList<>();
        List<Object[]> storeSeries = new ArrayList<>();
        for (TestData.Store st : catalog.stores()) {
            stores.add(new Object[]{st.id(), st.phoneNumber()});
            st.filmIds().forEach(id -> storeFilms.add(new Object[]{st.id(), id}));
            st.seriesIds().forEach(id -> storeSeries.add(new Object[]{st.id(), id}));
        }

        tx.executeWithoutResult(status -> {
            batch("insert into film (id, title, release_year, length) values (?, ?, ?, ?)", films);
            batch("insert into film_genres (film_id, genres) values (?, ?)", filmGenres);
            batch("insert into film_languages (film_id, languages) values (?, ?)", filmLanguages);
            batch("insert into series (id, title, release_year, episodes, length) values (?, ?, ?, ?, ?)", series);
            batch("insert into series_genres (series_id, genres) values (?, ?)", seriesGenres);
            batch("insert into series_languages (series_id, languages) values (?, ?)", seriesLanguages);
            batch("insert into store (id, phone_number) values (?, ?)", stores);
            batch("insert into store_films (store_id, film_id) values (?, ?)", storeFilms);
            batch("insert into store_series (store_id, series_id) values (?, ?)", storeSeries);
        });
    }

    public void writeCustomers(List<TestData.Customer> customers) {
        if (customers.isEmpty()) {
            return;
        }
        List<Object[]> customerRows = new ArrayList<>(customers.size());
        List<Object[]> billingRows = new ArrayList<>();
        List<Object[]> orderRows = new ArrayList<>();
        List<Object[]> placementRows = new ArrayList<>();
        for (TestData.Customer c : customers) {
            customerRows.add(new Object[]{c.id(), c.nickname(), c.email(), c.firstName(), c.lastName(),
                    c.street(), c.city(), c.zipCode(), c.country(), c.permission().name()});
            for (TestData.Billing b : c.billings()) {
                billingRows.add(new Object[]{b.id(), c.id(), b.cardNumber(), b.cvv(),
                        Date.valueOf(b.expirationDate())});
            }
            for (TestData.Placement p : c.placements()) {
                TestData.Order o = p.order();
                orderRows.add(new Object[]{o.id(), o.storeId(), Timestamp.valueOf(o.orderDate()), o.totalAmount()});
                placementRows.add(new Object[]{p.id(), c.id(), o.id(), Date.valueOf(p.orderDate()),
                        Date.valueOf(p.orderReturnDate()),
                        p.actualReturnDate() != null ? Date.valueOf(p.actualReturnDate()) : null});
            }
        }

        tx.executeWithoutResult(status -> {
            batch("insert into customer (id, nickname, email, first_name, last_name, address_street, "
                    + "address_city, address_zip_code, address_country, permission) "
                    + "values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", customerRows);
            batch("insert into billing_information (billing_information_id, customer_id, card_number, cvv, "
                    + "expiration_date) values (?, ?, ?, ?, ?)", billingRows);
            batch("insert into orders (id, store_id, order_date, total_amount) values (?, ?, ?, ?)", orderRows);
            batch("insert into order_placement (id, customer_id, order_id, order_date, order_return_date, "
                    + "actual_return_date) values (?, ?, ?, ?, ?, ?)", placementRows);
        });
    }

    /** zeilen anzahl pro tabelle für das log */
    public Map<String, Long> rowCounts() {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (String table : TABLES) {
            counts.put(table, jdbc.queryForObject("select count(*) from " + table, Long.class));
        }
        return counts;
    }

    private void batch(String sql, List<Object[]> rows) {
        if (!rows.isEmpty()) {
            jdbc.batchUpdate(sql, rows);
        }
    }
}
