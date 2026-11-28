package at.spengergasse.sj2526pos1javaseed.benchmark.write;

import at.spengergasse.sj2526pos1javaseed.nosql.model.CustomerDocument;
import at.spengergasse.sj2526pos1javaseed.nosql.ref.MongoReferencedRepository;
import at.spengergasse.sj2526pos1javaseed.testdata.TestData;
import at.spengergasse.sj2526pos1javaseed.testdata.TestDataDocumentMapper;
import at.spengergasse.sj2526pos1javaseed.testdata.TestDataGenerator;
import at.spengergasse.sj2526pos1javaseed.testdata.TestDataSizes;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * ein fertig vorbereiteter datensatz für eine skalierung
 *
 * - einmal pro skalierung mit dem testdaten generator erzeugt (fixer seed),
 *   dieselben objekte gehen an alle datenbanken und alle wiederholungen
 * - documents = die CustomerDocuments für mongo + mongo-ref, schon VOR der
 *   messung gemappt (mapping ist datenvorbereitung, keine persistenz)
 * - erwartete zeilen/dokument anzahlen für die prüfung nach jedem insert
 */
public record BenchmarkDataset(long seed, int chunkSize, TestDataSizes sizes, TestData.Catalog catalog,
                               List<TestData.Customer> customers, List<CustomerDocument> documents,
                               List<CustomerDocument.StoreJson> stores) {

    public static BenchmarkDataset generate(long seed, int scale, int chunkSize) {
        TestDataGenerator generator = new TestDataGenerator(seed, scale);
        TestData.Catalog catalog = generator.catalog();
        List<TestData.Customer> customers = generator.customers(0, scale);
        TestDataDocumentMapper mapper = new TestDataDocumentMapper(catalog);
        List<CustomerDocument> documents = customers.stream().map(mapper::toDocument).toList();
        return new BenchmarkDataset(seed, chunkSize, generator.sizes(), catalog, customers, documents,
                mapper.stores());
    }

    /** anzahl kunden = mongo dokumente = zeilen in customer */
    public int scale() {
        return sizes.customers();
    }

    public static <T> List<List<T>> chunks(List<T> items, int chunkSize) {
        List<List<T>> out = new ArrayList<>((items.size() + chunkSize - 1) / chunkSize);
        for (int from = 0; from < items.size(); from += chunkSize) {
            out.add(items.subList(from, Math.min(from + chunkSize, items.size())));
        }
        return out;
    }

    public long billingCount() {
        return customers.stream().mapToLong(c -> c.billings().size()).sum();
    }

    public long placementCount() {
        return customers.stream().mapToLong(c -> c.placements().size()).sum();
    }

    /** erwartete zeilen pro tabelle (alle tabellen aus RelationalTestDataWriter.TABLES) */
    public Map<String, Long> expectedRelationalRows() {
        Map<String, Long> m = new LinkedHashMap<>();
        m.put("order_placement", placementCount());
        m.put("orders", placementCount());
        m.put("billing_information", billingCount());
        m.put("customer", (long) scale());
        m.put("store_films", catalog.stores().stream().mapToLong(s -> s.filmIds().size()).sum());
        m.put("store_series", catalog.stores().stream().mapToLong(s -> s.seriesIds().size()).sum());
        m.put("store", (long) catalog.stores().size());
        m.put("film_genres", catalog.films().stream().mapToLong(f -> f.genres().size()).sum());
        m.put("film_languages", catalog.films().stream().mapToLong(f -> f.languages().size()).sum());
        m.put("film", (long) catalog.films().size());
        m.put("series_genres", catalog.series().stream().mapToLong(s -> s.genres().size()).sum());
        m.put("series_languages", catalog.series().stream().mapToLong(s -> s.languages().size()).sum());
        m.put("series", (long) catalog.series().size());
        return m;
    }

    /** erwartete dokumente pro ref_* collection */
    public Map<String, Long> expectedReferencedCounts() {
        Map<String, Long> rel = expectedRelationalRows();
        Map<String, Long> m = new LinkedHashMap<>();
        // collection namen aus dem bestehenden referenz repository, nicht doppelt pflegen
        m.put(MongoReferencedRepository.CUSTOMERS, (long) scale());
        m.put(MongoReferencedRepository.ADDRESSES, (long) scale());
        m.put(MongoReferencedRepository.BILLINGS, billingCount());
        m.put(MongoReferencedRepository.PLACEMENTS, placementCount());
        m.put(MongoReferencedRepository.ORDERS, placementCount());
        m.put(MongoReferencedRepository.STORES, rel.get("store"));
        m.put(MongoReferencedRepository.FILMS, rel.get("film"));
        m.put(MongoReferencedRepository.SERIES, rel.get("series"));
        m.put(MongoReferencedRepository.STORE_FILMS, rel.get("store_films"));
        m.put(MongoReferencedRepository.STORE_SERIES, rel.get("store_series"));
        return m;
    }
}
