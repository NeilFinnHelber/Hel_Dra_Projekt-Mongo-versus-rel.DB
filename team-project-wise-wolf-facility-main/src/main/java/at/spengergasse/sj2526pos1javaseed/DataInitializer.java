package at.spengergasse.sj2526pos1javaseed;

import at.spengergasse.sj2526pos1javaseed.Domain.*;
import at.spengergasse.sj2526pos1javaseed.Ids.*;
import at.spengergasse.sj2526pos1javaseed.persistence.*;
import at.spengergasse.sj2526pos1javaseed.richtypes.Email;
import at.spengergasse.sj2526pos1javaseed.richtypes.Phone_Number_Richtype;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * skaliersbarer seed für den mongo vs relational vergleich
 *
 * - pro lauf seed datensätze in allen kern tabellen
 *   inklusive m:n beziehungen (store_films, store_series)
 *   und element collections (film_genres, film_languages, ...)
 *
 * config (application.properties / env vars):
 * - app.seed.enabled    seed abschaltbar (default true)
 * - app.seed.count      basis skalierung, erzeugt ca 4-5x so viele
 *                       entities über alle tabellen
 *                       (default 1000, zielbereich 10-100000)
 * - app.seed.chunk-size persistenz chunk größe, nach jedem chunk
 *                       flush + clear damit der persistenzkontext
 *                       nicht vollläuft
 *
 * hinweis: entities werden über entityManager.persist geschrieben
 * nicht über JpaRepository.save, weil alle ids manuell vergeben sind
 * save() würde wegen den ids merge() aufrufen und pro entity ein
 * select ausführen - verfälscht die schreib zeiten
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private static final LocalDate BASE_DATE = LocalDate.of(2020, 1, 1);

    private static final Genre[] GENRES = Genre.values();
    private static final Language[] LANGUAGES = Language.values();
    private static final Country[] COUNTRIES = Country.values();
    private static final String[] FIRST_NAMES = {
            "Anna", "Ben", "Clara", "David", "Elena", "Felix", "Greta", "Hannes",
            "Ida", "Jonas", "Klara", "Lukas", "Mia", "Noah", "Olivia", "Paul"
    };
    private static final String[] LAST_NAMES = {
            "Huber", "Gruber", "Bauer", "Wagner", "Moser", "Pichler", "Steiner",
            "Mayer", "Hofer", "Leitner", "Berger", "Fuchs", "Eder", "Auer"
    };
    private static final String[] CITIES = {
            "Wien", "Graz", "Linz", "Salzburg", "Innsbruck", "Klagenfurt", "Villach"
    };

    private final FilmRepository filmRepository;
    private final SeriesRepository seriesRepository;
    private final CustomerRepository customerRepository;
    private final StoreRepository storeRepository;
    private final OrderRepository orderRepository;
    private final Order_PlacementRepository orderPlacementRepository;
    private final BillingInformationRepository billingInformationRepository;
    private final PlatformTransactionManager transactionManager;

    @PersistenceContext
    private EntityManager entityManager;

    @Value("${app.seed.enabled:true}")
    private boolean seedEnabled;

    @Value("${app.seed.count:1000}")
    private int seedCount;

    @Value("${app.seed.chunk-size:500}")
    private int chunkSize;

    @Override
    public void run(String... args) {
        if (!seedEnabled) {
            log.info("Seeding disabled (app.seed.enabled=false).");
            return;
        }
        if (filmRepository.count() > 0) {
            log.info("Database already contains data, skipping seed.");
            return;
        }

        int n = Math.max(1, seedCount);
        int filmCount = n;
        int seriesCount = Math.max(1, n / 10);
        int storeCount = Math.max(1, n / 100);
        int customerCount = n;
        int orderCount = n;
        int billingCount = Math.max(1, n / 10);

        log.info("Seeding started: count={} -> {} films, {} series, {} stores, {} customers, {} orders, {} billings",
                n, filmCount, seriesCount, storeCount, customerCount, orderCount, billingCount);

        long t0 = System.nanoTime();

        List<Long> filmIds = seedFilms(filmCount);
        List<Long> seriesIds = seedSeries(seriesCount);
        List<Long> storeIds = seedStores(storeCount, filmIds, seriesIds);
        List<Long> customerIds = seedCustomers(customerCount);
        List<Long> orderIds = seedOrders(orderCount, storeIds);
        seedOrderPlacements(orderCount, orderIds, customerIds);
        seedBillingInformation(billingCount, customerIds);

        transactionManager.execute(status -> {
            entityManager.flush();
            entityManager.clear();
            return null;
        });

        log.info("Seeding finished in {} ms", (System.nanoTime() - t0) / 1_000_000);
    }

    // ---------- Film & Series ----------

    private List<Long> seedFilms(int count) {
        List<Long> ids = new ArrayList<>(count);
        List<Film> chunk = new ArrayList<>(chunkSize);
        for (int i = 0; i < count; i++) {
            long id = i + 1L;
            ids.add(id);
            chunk.add(Film.builder()
                    .filmid(new FilmId(id))
                    .title("Film " + id)
                    .release_year(1990 + i % 35)
                    .length(90f + (i % 100))
                    .languages(languageSet(i))
                    .genres(genreSet(i))
                    .build());
            if (chunk.size() >= chunkSize) flushChunk(chunk);
        }
        flushChunk(chunk);
        return ids;
    }

    private List<Long> seedSeries(int count) {
        List<Long> ids = new ArrayList<>(count);
        List<Series> chunk = new ArrayList<>(chunkSize);
        for (int i = 0; i < count; i++) {
            long id = i + 1L;
            ids.add(id);
            chunk.add(Series.builder()
                    .seriesid(new SeriesId(id))
                    .title("Series " + id)
                    .release_year(1990 + i % 35)
                    .episodes(6 + i % 60)
                    .length(40f + (i % 20))
                    .languages(languageSet(i + 3))
                    .genres(genreSet(i + 3))
                    .build());
            if (chunk.size() >= chunkSize) flushChunk(chunk);
        }
        flushChunk(chunk);
        return ids;
    }

    // ---------- Store (m:n auf Film & Series) ----------

    private List<Long> seedStores(int count, List<Long> filmIds, List<Long> seriesIds) {
        List<Long> ids = new ArrayList<>(count);
        List<Store> chunk = new ArrayList<>(chunkSize);
        for (int i = 0; i < count; i++) {
            long id = i + 1L;
            ids.add(id);

            Set<Film> films = new LinkedHashSet<>();
            Set<Series> series = new LinkedHashSet<>();
            for (int k = 0; k < 10; k++) {
                films.add(filmRef(pick(filmIds, i * 31 + k * 97)));
                series.add(seriesRef(pick(seriesIds, i * 17 + k * 53)));
            }

            chunk.add(Store.builder()
                    .storeId(new StoreId(id))
                    .phoneNumber(new Phone_Number_Richtype("+43 1 555" + String.format("%05d", i)))
                    .films(films)
                    .series(series)
                    .build());
            if (chunk.size() >= chunkSize) flushChunk(chunk);
        }
        flushChunk(chunk);
        return ids;
    }

    // ---------- Customer ----------

    private List<Long> seedCustomers(int count) {
        List<Long> ids = new ArrayList<>(count);
        List<Customer> chunk = new ArrayList<>(chunkSize);
        for (int i = 0; i < count; i++) {
            long id = i + 1L;
            ids.add(id);
            chunk.add(Customer.builder()
                    .id(new UserId(id))
                    .nickname("seed.user." + id)
                    .firstName(FIRST_NAMES[i % FIRST_NAMES.length])
                    .lastName(LAST_NAMES[i % LAST_NAMES.length] + id)
                    .email(new Email("seed.user." + id + "@example.com"))
                    .address(new Address(
                            "Seedstraße " + (i % 200 + 1),
                            CITIES[i % CITIES.length],
                            String.format("%04d", 1000 + i % 9000),
                            COUNTRIES[i % COUNTRIES.length].name()))
                    .permission(i % 10 == 0 ? Permission.Admin : i % 10 == 1 ? Permission.Employee : Permission.Customer)
                    .build());
            if (chunk.size() >= chunkSize) flushChunk(chunk);
        }
        flushChunk(chunk);
        return ids;
    }

    // ---------- Order & Order_placement ----------

    private List<Long> seedOrders(int count, List<Long> storeIds) {
        List<Long> ids = new ArrayList<>(count);
        List<Order> chunk = new ArrayList<>(chunkSize);
        for (int i = 0; i < count; i++) {
            long id = i + 1L;
            ids.add(id);
            chunk.add(Order.builder()
                    .id(new OrderId(id))
                    .orderDate(BASE_DATE.plusDays(i % 3650).atStartOfDay().plusHours(i % 24))
                    .totalAmount(BigDecimal.valueOf(5 + (i * 37) % 500).setScale(2, RoundingMode.HALF_UP))
                    .store(storeRef(pick(storeIds, i * 13)))
                    .build());
            if (chunk.size() >= chunkSize) flushChunk(chunk);
        }
        flushChunk(chunk);
        return ids;
    }

    private void seedOrderPlacements(int count, List<Long> orderIds, List<Long> customerIds) {
        List<Order_Placement> chunk = new ArrayList<>(chunkSize);
        for (int i = 0; i < count; i++) {
            long id = i + 1L;
            LocalDate orderDate = BASE_DATE.plusDays(i % 365);
            chunk.add(Order_Placement.builder()
                    .id(new Order_PlacementId(id))
                    .orderDate(orderDate)
                    .orderReturnDate(orderDate.plusDays(14))
                    .actualReturnDate(i % 3 == 0 ? null : orderDate.plusDays(10))
                    .customer(customerRef(pick(customerIds, i * 7 + 1)))
                    .order(orderRef(pick(orderIds, i)))
                    .build());
            if (chunk.size() >= chunkSize) flushChunk(chunk);
        }
        flushChunk(chunk);
    }

    // ---------- Billing_Information ----------

    private void seedBillingInformation(int count, List<Long> customerIds) {
        List<BillingInformation> chunk = new ArrayList<>(chunkSize);
        for (int i = 0; i < count; i++) {
            long customerId = pick(customerIds, i * 11);
            chunk.add(BillingInformation.builder()
                    .id(new BillingInformationId(i + 1L))
                    .cardNumber(String.format("%016d", 4000000000000000L + i))
                    .cvv(100 + i % 900)
                    .expirationDate(LocalDate.of(2027 + i % 3, 1 + i % 12, 1))
                    .customer(customerRef(customerId))
                    .build());
            if (chunk.size() >= chunkSize) flushChunk(chunk);
        }
        flushChunk(chunk);
    }

    // ---------- Helpers ----------

    private <T> void flushChunk(List<T> chunk) {
        if (chunk.isEmpty()) return;
        transactionManager.executeWithoutResult(status -> {
            for (T entity : chunk) {
                entityManager.persist(entity);
            }
            entityManager.flush();
        });
        entityManager.clear();
        chunk.clear();
    }

    private static long pick(List<Long> ids, long salt) {
        return ids.get((int) Math.floorMod(salt, ids.size()));
    }

    private static Set<Genre> genreSet(int i) {
        return Set.of(GENRES[i % GENRES.length], GENRES[(i + 5) % GENRES.length]);
    }

    private static Set<Language> languageSet(int i) {
        return Set.of(LANGUAGES[i % LANGUAGES.length], LANGUAGES[(i + 2) % LANGUAGES.length]);
    }

    // referenz-stubs: nur die id wird für fk/join inserts gebraucht
    // werden nie persistiert und nach clear() auch nicht mehr benutzt

    private static Film filmRef(long id) {
        return Film.builder().filmid(new FilmId(id)).build();
    }

    private static Series seriesRef(long id) {
        return Series.builder().seriesid(new SeriesId(id)).build();
    }

    private static Store storeRef(long id) {
        return Store.builder().storeId(new StoreId(id)).build();
    }

    private static Customer customerRef(long id) {
        return Customer.builder().id(new UserId(id)).build();
    }

    private static Order orderRef(long id) {
        return Order.builder().id(new OrderId(id)).build();
    }
}
