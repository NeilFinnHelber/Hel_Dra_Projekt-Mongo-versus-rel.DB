package at.spengergasse.sj2526pos1javaseed.testdata;

import at.spengergasse.sj2526pos1javaseed.nosql.model.CustomerDocument;
import at.spengergasse.sj2526pos1javaseed.richtypes.Email;
import at.spengergasse.sj2526pos1javaseed.richtypes.Phone_Number_Richtype;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * generator tests ohne datenbank:
 * reproduzierbarkeit, seed abhängigkeit, chunk unabhängigkeit,
 * referenzielle integrität und unique constraints
 */
class TestDataGeneratorTest {

    @Test
    void sameSeedProducesIdenticalDataset() {
        TestDataGenerator a = new TestDataGenerator(42, 1000);
        TestDataGenerator b = new TestDataGenerator(42, 1000);

        assertThat(a.catalog()).isEqualTo(b.catalog());
        assertThat(a.customers(0, 1000)).isEqualTo(b.customers(0, 1000));
    }

    @Test
    void differentSeedProducesDifferentDataset() {
        TestDataGenerator a = new TestDataGenerator(42, 1000);
        TestDataGenerator b = new TestDataGenerator(43, 1000);

        assertThat(a.catalog()).isNotEqualTo(b.catalog());
        List<TestData.Customer> ca = a.customers(0, 1000);
        List<TestData.Customer> cb = b.customers(0, 1000);
        long sameNames = 0;
        for (int i = 0; i < ca.size(); i++) {
            if (ca.get(i).nickname().equals(cb.get(i).nickname())) sameNames++;
        }
        // nicht nur "irgendwo anders", sondern praktisch alle datensätze
        assertThat(sameNames).isLessThan(50);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 7, 333, 1000})
    void chunkSizeDoesNotChangeData(int chunkSize) {
        TestDataGenerator reference = new TestDataGenerator(7, 1000);
        List<TestData.Customer> expected = reference.customers(0, 1000);

        TestDataGenerator chunked = new TestDataGenerator(7, 1000);
        List<TestData.Customer> actual = new ArrayList<>();
        for (int from = 0; from < 1000; from += chunkSize) {
            actual.addAll(chunked.customers(from, Math.min(from + chunkSize, 1000)));
        }
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void generationOrderDoesNotChangeData() {
        TestDataGenerator g = new TestDataGenerator(7, 100);
        List<TestData.Customer> tail = g.customers(50, 100);
        g.catalog();
        List<TestData.Customer> head = g.customers(0, 50);

        List<TestData.Customer> all = new TestDataGenerator(7, 100).customers(0, 100);
        assertThat(head).isEqualTo(all.subList(0, 50));
        assertThat(tail).isEqualTo(all.subList(50, 100));
    }

    @ParameterizedTest
    @ValueSource(ints = {10, 100, 1000, 20_000})
    void referencesAndUniqueConstraintsAreValid(int count) {
        TestDataGenerator g = new TestDataGenerator(42, count);
        TestData.Catalog catalog = g.catalog();
        List<TestData.Customer> customers = g.customers(0, count);
        TestDataSizes sizes = g.sizes();

        assertThat(catalog.films()).hasSize(sizes.films());
        assertThat(catalog.series()).hasSize(sizes.series());
        assertThat(catalog.stores()).hasSize(sizes.stores());
        assertThat(customers).hasSize(count);

        Set<Long> filmIds = ids(catalog.films(), TestData.Film::id);
        Set<Long> seriesIds = ids(catalog.series(), TestData.Series::id);
        Set<Long> storeIds = ids(catalog.stores(), TestData.Store::id);

        // m:n store_films / store_series: nur gültige ids, keine duplikate (pk film_id+store_id)
        Set<Long> filmsInStores = new HashSet<>();
        Set<Long> seriesInStores = new HashSet<>();
        for (TestData.Store s : catalog.stores()) {
            assertThat(s.filmIds()).doesNotHaveDuplicates().hasSize(sizes.filmsPerStore());
            assertThat(s.seriesIds()).doesNotHaveDuplicates().hasSize(sizes.seriesPerStore());
            assertThat(filmIds).containsAll(s.filmIds());
            assertThat(seriesIds).containsAll(s.seriesIds());
            filmsInStores.addAll(s.filmIds());
            seriesInStores.addAll(s.seriesIds());
            new Phone_Number_Richtype(s.phoneNumber());
        }
        // vollständigkeit: jeder titel taucht auch im mongo modell auf
        assertThat(filmsInStores).isEqualTo(filmIds);
        assertThat(seriesInStores).isEqualTo(seriesIds);

        Set<Long> customerIds = new HashSet<>();
        Set<String> nicknames = new HashSet<>();
        Set<String> emails = new HashSet<>();
        Set<Long> billingIds = new HashSet<>();
        Set<Long> orderIds = new HashSet<>();
        Set<Long> placementIds = new HashSet<>();
        Set<Long> storesWithOrders = new HashSet<>();
        for (TestData.Customer c : customers) {
            assertThat(customerIds.add(c.id())).isTrue();
            assertThat(nicknames.add(c.nickname())).isTrue();
            assertThat(emails.add(c.email())).isTrue();
            new Email(c.email()); // gleiche validierung wie die entity
            assertThat(c.firstName()).isNotBlank();
            assertThat(c.lastName()).isNotBlank();
            assertThat(c.street()).isNotBlank();
            assertThat(c.zipCode()).matches("\\d{4}");
            assertThat(c.billings()).hasSizeBetween(1, 2);
            assertThat(c.placements()).hasSizeBetween(1, 3);

            for (TestData.Billing b : c.billings()) {
                assertThat(billingIds.add(b.id())).isTrue();
                assertThat(b.cardNumber()).matches("\\d{16}");
                assertThat(luhnValid(b.cardNumber())).isTrue();
                assertThat(b.cvv()).isBetween(100, 999);
            }
            for (TestData.Placement p : c.placements()) {
                assertThat(placementIds.add(p.id())).isTrue();
                assertThat(orderIds.add(p.order().id())).isTrue(); // order_placement.order_id unique
                // außerhalb vom id bereich den BenchmarkRunner für seine orders nutzt
                assertThat(p.order().id()).isGreaterThan(TestDataGenerator.ORDER_ID_OFFSET);
                assertThat(storeIds).contains(p.order().storeId());
                storesWithOrders.add(p.order().storeId());
                assertThat(p.orderReturnDate()).isAfter(p.orderDate());
                if (p.actualReturnDate() != null) {
                    assertThat(p.actualReturnDate()).isAfter(p.orderDate());
                }
                assertThat(p.orderDate()).isEqualTo(p.order().orderDate().toLocalDate());
                assertThat(p.order().totalAmount().scale()).isEqualTo(2);
                assertThat(p.order().totalAmount().precision()).isLessThanOrEqualTo(10);
            }
        }
        // jeder store hat mind. eine order -> kommt im mongo modell vor
        assertThat(storesWithOrders).isEqualTo(storeIds);
    }

    @Test
    void dataIsVaried() {
        List<TestData.Customer> customers = new TestDataGenerator(42, 1000).customers(0, 1000);
        assertThat(customers.stream().map(TestData.Customer::firstName).distinct().count()).isGreaterThan(100);
        assertThat(customers.stream().map(TestData.Customer::lastName).distinct().count()).isGreaterThan(100);
        assertThat(customers.stream().map(TestData.Customer::city).distinct().count()).isGreaterThan(15);
        assertThat(customers.stream().map(TestData.Customer::permission).distinct().count()).isEqualTo(3);

        TestData.Catalog catalog = new TestDataGenerator(42, 100_000).catalog();
        assertThat(catalog.films().stream().map(TestData.Film::title).distinct().count()).isGreaterThan(200);
    }

    @ParameterizedTest
    @CsvSource({
            "10,     3,  20,  10, 10, 5",
            "100,    3,  20,  10, 10, 5",
            "1000,   3,  20,  10, 10, 5",
            "10000,  10, 50,  25, 10, 5",
            "100000, 100, 500, 250, 10, 5"
    })
    void sizesScaleWithCustomerCount(int customers, int stores, int films, int series,
                                     int filmsPerStore, int seriesPerStore) {
        assertThat(TestDataSizes.forCustomers(customers))
                .isEqualTo(new TestDataSizes(customers, films, series, stores, filmsPerStore, seriesPerStore));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 9, 100_001})
    void rejectsCountOutsideSupportedRange(int count) {
        assertThatThrownBy(() -> new TestDataGenerator(42, count))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("10")
                .hasMessageContaining("100000");
    }

    @Test
    void documentEmbedsFullStoreCatalogOfReferencedStore() {
        TestDataGenerator g = new TestDataGenerator(42, 100);
        TestData.Catalog catalog = g.catalog();
        TestData.Customer customer = g.customers(0, 1).getFirst();
        CustomerDocument doc = new TestDataDocumentMapper(catalog).toDocument(customer);

        assertThat(doc.id()).isEqualTo(customer.id());
        assertThat(doc.email()).isEqualTo(customer.email());
        assertThat(doc.address().city()).isEqualTo(customer.city());
        assertThat(doc.billingInformation()).hasSize(customer.billings().size());
        assertThat(doc.orderPlacements()).hasSize(customer.placements().size());

        Map<Long, TestData.Store> stores = catalog.stores().stream()
                .collect(Collectors.toMap(TestData.Store::id, Function.identity()));
        for (int i = 0; i < customer.placements().size(); i++) {
            TestData.Placement p = customer.placements().get(i);
            CustomerDocument.PlacementJson json = doc.orderPlacements().get(i);
            assertThat(json.order().orderId()).isEqualTo(p.order().id());
            assertThat(json.order().totalAmount()).isEqualTo(p.order().totalAmount());
            assertThat(json.actualReturnDate()).isEqualTo(p.actualReturnDate());
            TestData.Store store = stores.get(p.order().storeId());
            assertThat(json.order().store().storeId()).isEqualTo(store.id());
            assertThat(json.order().store().films()).extracting(CustomerDocument.FilmJson::filmId)
                    .containsExactlyElementsOf(store.filmIds());
            assertThat(json.order().store().series()).extracting(CustomerDocument.SeriesJson::seriesId)
                    .containsExactlyElementsOf(store.seriesIds());
        }
    }

    @Test
    void slugProducesEmailSafeAscii() {
        assertThat(TestDataGenerator.slug("Jürgen")).isEqualTo("juergen");
        assertThat(TestDataGenerator.slug("Größ-Müller")).isEqualTo("groessmueller");
        assertThat(TestDataGenerator.slug("Zoë")).isEqualTo("zoe");
        assertThat(TestDataGenerator.slug("---")).isEqualTo("user");
    }

    @Test
    void hostParsingForSafetyCheck() {
        assertThat(TestDataService.postgresHosts("jdbc:postgresql://localhost:15432/db?x=1"))
                .containsExactly("localhost");
        assertThat(TestDataService.postgresHosts("jdbc:postgresql://db1.prod:5432,db2.prod:5432/db"))
                .containsExactly("db1.prod", "db2.prod");
        assertThat(TestDataService.postgresHosts("jdbc:postgresql://[::1]:5432/db")).containsExactly("::1");
        assertThat(TestDataService.postgresHosts("jdbc:h2:mem:x")).isEmpty();
        assertThat(TestDataService.mongoHosts("mongodb://user:pw@mongo:27017/?authSource=admin"))
                .containsExactly("mongo");
    }

    private static <T> Set<Long> ids(List<T> list, Function<T, Long> id) {
        return list.stream().map(id).collect(Collectors.toSet());
    }

    private static boolean luhnValid(String number) {
        int sum = 0;
        for (int i = 0; i < number.length(); i++) {
            int d = number.charAt(number.length() - 1 - i) - '0';
            if (i % 2 == 1) {
                d *= 2;
                if (d > 9) d -= 9;
            }
            sum += d;
        }
        return sum % 10 == 0;
    }
}
