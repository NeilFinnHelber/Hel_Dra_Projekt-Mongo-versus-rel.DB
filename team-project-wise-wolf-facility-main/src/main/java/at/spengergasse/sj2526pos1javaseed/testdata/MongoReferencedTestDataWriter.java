package at.spengergasse.sj2526pos1javaseed.testdata;

import at.spengergasse.sj2526pos1javaseed.nosql.model.CustomerDocument;
import at.spengergasse.sj2526pos1javaseed.nosql.ref.MongoReferencedRepository;
import com.mongodb.client.MongoDatabase;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * schreibt testdaten ins mongo referenz modell (ref_* collections)
 *
 * - gleiche CustomerDocuments wie das embedded modell (TestDataDocumentMapper),
 *   normalisiert wird im bestehenden MongoReferencedRepository
 * - katalog (stores, films, series, store_films/store_series) einmal pro
 *   datensatz über insertCatalog, kunden pro chunk über insertCustomers
 *   (insertBatch würde den katalog in jedem chunk erneut schreiben -> duplicate key)
 * - keine spring bean: wird vom write benchmark gebaut
 */
public class MongoReferencedTestDataWriter {

    /**
     * insertMany größe: ein chunk soll pro collection genau ein insertMany sein
     * (wie ein jdbc batch pro tabelle im relationalen writer). pro kunde gibt es
     * max. 3 placements/orders und 2 billings, der katalog bleibt < 10.000 docs
     */
    private static final int CHILDREN_PER_CUSTOMER = 3;
    private static final int CATALOG_BATCH = 10_000;

    private final MongoDatabase database;
    private final MongoReferencedRepository repository;

    public MongoReferencedTestDataWriter(MongoDatabase database, MongoReferencedRepository repository) {
        this.database = database;
        this.repository = repository;
    }

    /**
     * destruktiv: droppt alle ref_* collections (inkl. indexe) und legt sie
     * leer neu an, damit das anlegen nicht im ersten insert passiert
     */
    public void wipe() {
        for (String name : MongoReferencedRepository.ALL_COLLECTIONS) {
            database.getCollection(name).drop();
            database.createCollection(name);
        }
    }

    public void writeCatalog(List<CustomerDocument.StoreJson> stores) {
        if (!stores.isEmpty()) {
            repository.insertCatalog(stores, CATALOG_BATCH);
        }
    }

    /** ein chunk kunden, normalisiert auf customers/addresses/billings/placements/orders */
    public void writeDocuments(List<CustomerDocument> docs) {
        if (!docs.isEmpty()) {
            repository.insertCustomers(docs, docs.size() * CHILDREN_PER_CUSTOMER);
        }
    }

    /** anzahl dokumente pro ref_* collection */
    public Map<String, Long> counts() {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (String name : MongoReferencedRepository.ALL_COLLECTIONS) {
            counts.put(name, database.getCollection(name).countDocuments());
        }
        return counts;
    }
}
