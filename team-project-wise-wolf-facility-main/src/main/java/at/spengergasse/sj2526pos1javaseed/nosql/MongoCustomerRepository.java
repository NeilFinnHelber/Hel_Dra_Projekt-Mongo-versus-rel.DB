package at.spengergasse.sj2526pos1javaseed.nosql;

import at.spengergasse.sj2526pos1javaseed.benchmark.CustomerProjection;
import at.spengergasse.sj2526pos1javaseed.benchmark.PersistenceRepository;
import at.spengergasse.sj2526pos1javaseed.nosql.model.CustomerDocument;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.model.Updates;
import org.bson.Document;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

/**
 * mongo variante vom benchmark interface
 *
 * - offizieller mongo sync treiber, kein spring data kram
 * - CustomerDocument <-> json <-> bson Document über jackson
 *   (json modell ist die einzige quelle der wahrheit)
 * - gleiche operationen wie die jpa variante, damit vergleichbar
 */
@Repository
public class MongoCustomerRepository implements PersistenceRepository {

    static final String COLLECTION_NAME = "customers";

    private final MongoCollection<Document> customers;
    private final ObjectMapper objectMapper;

    public MongoCustomerRepository(MongoDatabase mongoDatabase, ObjectMapper objectMapper) {
        this.customers = mongoDatabase.getCollection(COLLECTION_NAME);
        this.objectMapper = objectMapper;
    }

    @Override
    public long insertBatch(List<CustomerDocument> documents, int batchSize) {
        List<Document> batch = new ArrayList<>(batchSize);
        for (CustomerDocument doc : documents) {
            batch.add(toDocument(doc));
            if (batch.size() >= batchSize) {
                customers.insertMany(batch);
                batch.clear();
            }
        }
        if (!batch.isEmpty()) {
            customers.insertMany(batch);
        }
        return documents.size();
    }

    @Override
    public List<CustomerDocument> findAll(int limit) {
        return customers.find()
                .limit(limit)
                .map(this::toDocumentModel)
                .into(new ArrayList<>());
    }

    @Override
    public List<CustomerDocument> findByCity(String city, int limit) {
        return customers.find(Filters.eq("address.city", city))
                .limit(limit)
                .map(this::toDocumentModel)
                .into(new ArrayList<>());
    }

    @Override
    public List<CustomerProjection> findByCityProjection(String city, int limit) {
        return customers.find(Filters.eq("address.city", city))
                .projection(Projections.include("_id", "nickname", "address.city"))
                .limit(limit)
                .map(this::toProjection)
                .into(new ArrayList<>());
    }

    @Override
    public List<CustomerProjection> findByCityProjectionSorted(String city, boolean ascending, int limit) {
        return customers.find(Filters.eq("address.city", city))
                .projection(Projections.include("_id", "nickname", "address.city"))
                .sort(ascending ? Sorts.ascending("nickname") : Sorts.descending("nickname"))
                .limit(limit)
                .map(this::toProjection)
                .into(new ArrayList<>());
    }

    @Override
    public long updateLastNameByNickname(String nickname, String lastName) {
        return customers.updateOne(
                Filters.eq("nickname", nickname),
                Updates.set("lastName", lastName)).getModifiedCount();
    }

    @Override
    public long updateLastNameAll(String lastName) {
        return customers.updateMany(
                new Document(),
                Updates.set("lastName", lastName)).getModifiedCount();
    }

    @Override
    public long deleteByCity(String city) {
        return customers.deleteMany(Filters.eq("address.city", city)).getDeletedCount();
    }

    // ---------- Mapping ----------

    private Document toDocument(CustomerDocument doc) {
        try {
            Document bson = Document.parse(objectMapper.writeValueAsString(doc));
            // mongo generiert _id selbst, also weghauen wenn null/leer
            if (bson.get("_id") == null) {
                bson.remove("_id");
            }
            return bson;
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot serialize CustomerDocument", e);
        }
    }

    private CustomerDocument toDocumentModel(Document bson) {
        try {
            return objectMapper.readValue(bson.toJson(), CustomerDocument.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot deserialize Mongo document", e);
        }
    }

    private CustomerProjection toProjection(Document bson) {
        Object id = bson.get("_id");
        Long idLong = id instanceof Number n ? n.longValue() : null;
        String nickname = bson.getString("nickname");
        Document address = bson.get("address", Document.class);
        String city = address != null ? address.getString("city") : null;
        return new CustomerProjection(idLong, nickname, city);
    }
}
