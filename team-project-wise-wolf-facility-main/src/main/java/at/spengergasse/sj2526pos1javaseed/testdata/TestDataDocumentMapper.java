package at.spengergasse.sj2526pos1javaseed.testdata;

import at.spengergasse.sj2526pos1javaseed.nosql.model.CustomerDocument;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * TestData -> CustomerDocument (mongo modell, 100% embedded)
 *
 * - store inkl. kompletten film/series katalog wird pro placement
 *   eingebettet, genau wie im bestehenden dokumentmodell
 * - StoreJson wird pro store nur einmal gebaut und von allen
 *   dokumenten geteilt -> im java heap kein vielfacher katalog,
 *   die duplizierung passiert erst beim serialisieren nach bson
 */
public class TestDataDocumentMapper {

    private final Map<Long, CustomerDocument.StoreJson> storesById;

    public TestDataDocumentMapper(TestData.Catalog catalog) {
        Map<Long, CustomerDocument.FilmJson> films = catalog.films().stream()
                .collect(Collectors.toMap(TestData.Film::id, TestDataDocumentMapper::toFilmJson));
        Map<Long, CustomerDocument.SeriesJson> series = catalog.series().stream()
                .collect(Collectors.toMap(TestData.Series::id, TestDataDocumentMapper::toSeriesJson));
        Map<Long, CustomerDocument.StoreJson> stores = new HashMap<>();
        for (TestData.Store store : catalog.stores()) {
            stores.put(store.id(), new CustomerDocument.StoreJson(
                    store.id(),
                    store.phoneNumber(),
                    resolve(store.filmIds(), films::get),
                    resolve(store.seriesIds(), series::get)));
        }
        this.storesById = Map.copyOf(stores);
    }

    /** alle stores inkl. katalog, nach id sortiert (für den katalog insert im referenz modell) */
    public List<CustomerDocument.StoreJson> stores() {
        return storesById.values().stream()
                .sorted(java.util.Comparator.comparingLong(CustomerDocument.StoreJson::storeId))
                .toList();
    }

    public CustomerDocument toDocument(TestData.Customer c) {
        return new CustomerDocument(
                c.id(),
                c.nickname(),
                c.email(),
                c.firstName(),
                c.lastName(),
                c.permission().name(),
                new CustomerDocument.AddressJson(c.street(), c.city(), c.zipCode(), c.country()),
                c.billings().stream()
                        .map(b -> new CustomerDocument.BillingJson(
                                b.cardNumber(), b.expirationDate().toString(), b.cvv()))
                        .toList(),
                c.placements().stream()
                        .map(this::toPlacementJson)
                        .toList());
    }

    private CustomerDocument.PlacementJson toPlacementJson(TestData.Placement p) {
        TestData.Order o = p.order();
        CustomerDocument.StoreJson store = storesById.get(o.storeId());
        if (store == null) {
            throw new IllegalStateException("order " + o.id() + " referenziert unbekannten store " + o.storeId());
        }
        return new CustomerDocument.PlacementJson(
                new CustomerDocument.OrderJson(o.id(), o.orderDate(), o.totalAmount(), store),
                p.orderDate(),
                p.orderReturnDate(),
                p.actualReturnDate());
    }

    private static CustomerDocument.FilmJson toFilmJson(TestData.Film f) {
        return new CustomerDocument.FilmJson(f.id(), f.title(), f.releaseYear(), f.length(),
                names(f.languages()), names(f.genres()));
    }

    private static CustomerDocument.SeriesJson toSeriesJson(TestData.Series s) {
        return new CustomerDocument.SeriesJson(s.id(), s.title(), s.releaseYear(), s.episodes(), s.length(),
                names(s.languages()), names(s.genres()));
    }

    private static <T> List<T> resolve(List<Long> ids, Function<Long, T> lookup) {
        return ids.stream().map(id -> {
            T value = lookup.apply(id);
            if (value == null) {
                throw new IllegalStateException("store referenziert unbekannte id " + id);
            }
            return value;
        }).toList();
    }

    private static List<String> names(List<? extends Enum<?>> values) {
        return values.stream().map(Enum::name).toList();
    }
}
