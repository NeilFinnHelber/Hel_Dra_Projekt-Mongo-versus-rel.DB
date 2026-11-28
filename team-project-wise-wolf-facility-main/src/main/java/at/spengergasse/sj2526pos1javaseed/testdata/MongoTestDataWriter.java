package at.spengergasse.sj2526pos1javaseed.testdata;

import at.spengergasse.sj2526pos1javaseed.nosql.MongoCustomerRepository;
import at.spengergasse.sj2526pos1javaseed.nosql.model.CustomerDocument;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

import java.util.List;

/**
 * schreibt TestData in mongo
 *
 * - mapping über TestDataDocumentMapper auf das bestehende CustomerDocument
 * - insert über das bestehende MongoCustomerRepository.insertBatch
 *   (gleiche json -> bson konvertierung wie im benchmark)
 * - keine spring bean: wird vom TestDataService bzw. vom write benchmark gebaut
 */
public class MongoTestDataWriter {

    private final MongoDatabase database;
    private final MongoCustomerRepository repository;
    private TestDataDocumentMapper mapper;

    public MongoTestDataWriter(MongoDatabase database, MongoCustomerRepository repository) {
        this.database = database;
        this.repository = repository;
    }

    /**
     * destruktiv: droppt die komplette customers collection (inkl. indexe)
     * und legt sie leer neu an, damit das anlegen nicht im ersten insert
     * (und damit in einer zeitmessung) passiert
     */
    public void wipe() {
        database.getCollection(MongoCustomerRepository.COLLECTION_NAME).drop();
        database.createCollection(MongoCustomerRepository.COLLECTION_NAME);
    }

    /** katalog wird nicht separat gespeichert, sondern pro placement eingebettet */
    public void prepareCatalog(TestData.Catalog catalog) {
        this.mapper = new TestDataDocumentMapper(catalog);
    }

    public void writeCustomers(List<TestData.Customer> customers) {
        if (mapper == null) {
            throw new IllegalStateException("prepareCatalog muss vor writeCustomers aufgerufen werden");
        }
        if (customers.isEmpty()) {
            return;
        }
        writeDocuments(customers.stream().map(mapper::toDocument).toList());
    }

    /** bereits gemappte dokumente schreiben (ein insertMany pro aufruf), z.b. im write benchmark */
    public void writeDocuments(List<CustomerDocument> docs) {
        if (!docs.isEmpty()) {
            repository.insertBatch(docs, docs.size());
        }
    }

    /** anzahl dokumente in der customers collection */
    public long count() {
        return database.getCollection(MongoCustomerRepository.COLLECTION_NAME).countDocuments();
    }

    /** kurzer überblick für das log: dokumente, placements, avg dokument größe */
    public String summary() {
        Document stats = database.runCommand(new Document("collStats", MongoCustomerRepository.COLLECTION_NAME));
        long count = ((Number) stats.get("count")).longValue();
        Number avg = (Number) stats.get("avgObjSize");
        Number size = (Number) stats.get("size");
        Number storage = (Number) stats.get("storageSize");
        Document placements = database.getCollection(MongoCustomerRepository.COLLECTION_NAME)
                .aggregate(List.of(new Document("$group", new Document("_id", null)
                        .append("n", new Document("$sum", new Document("$size", "$orderPlacements"))))))
                .first();
        long placementCount = placements != null ? ((Number) placements.get("n")).longValue() : 0;
        return String.format("documents=%d, embedded placements=%d, avgDocSize=%d B, dataSize=%.1f MB, storageSize=%.1f MB",
                count, placementCount,
                avg != null ? avg.longValue() : 0,
                size != null ? size.doubleValue() / 1_048_576 : 0,
                storage != null ? storage.doubleValue() / 1_048_576 : 0);
    }
}
