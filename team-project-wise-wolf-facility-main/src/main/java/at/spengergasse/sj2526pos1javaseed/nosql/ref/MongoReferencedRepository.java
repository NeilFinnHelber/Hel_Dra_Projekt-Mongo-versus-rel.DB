package at.spengergasse.sj2526pos1javaseed.nosql.ref;

import at.spengergasse.sj2526pos1javaseed.benchmark.CustomerProjection;
import at.spengergasse.sj2526pos1javaseed.benchmark.PersistenceRepository;
import at.spengergasse.sj2526pos1javaseed.nosql.model.CustomerDocument;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Accumulators;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.model.UnwindOptions;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * mongo referenz/modell variante vom benchmark interface
 *
 * - normalisiert wie die relationale db: eine collection pro entitaet,
 *   verknuepft ueber _id referenzen (analog zu fremdschluesseln)
 * - m:n beziehungen (store <-> films/series) als eigene join collections
 *   ref_store_films / ref_store_series
 * - reads die mehrere collections brauchen laufen ueber
 *   $lookup aggregation (projection finds) oder gebuendelte $in queries
 *   (hydration der vollen customer docs)
 * - eingabe/ausgabe ist dasselbe CustomerDocument interface wie beim
 *   embedded modell, damit die ergebnisse vergleichbar sind
 *
 * collections (alle im selben mongo database, prefix ref_):
 *   ref_customers, ref_addresses, ref_billings,
 *   ref_order_placements, ref_orders, ref_stores,
 *   ref_films, ref_series, ref_store_films, ref_store_series
 */
@Repository
public class MongoReferencedRepository implements PersistenceRepository {

    // collection namen
    public static final String CUSTOMERS = "ref_customers";
    public static final String ADDRESSES = "ref_addresses";
    public static final String BILLINGS = "ref_billings";
    public static final String PLACEMENTS = "ref_order_placements";
    public static final String ORDERS = "ref_orders";
    public static final String STORES = "ref_stores";
    public static final String FILMS = "ref_films";
    public static final String SERIES = "ref_series";
    public static final String STORE_FILMS = "ref_store_films";
    public static final String STORE_SERIES = "ref_store_series";

    /** alle collection namen, fuer reset */
    public static final List<String> ALL_COLLECTIONS = List.of(
            CUSTOMERS, ADDRESSES, BILLINGS, PLACEMENTS, ORDERS,
            STORES, FILMS, SERIES, STORE_FILMS, STORE_SERIES);

    /** $in listen werden in chuncks gesplittet, damit bson limit nicht reißt */
    private static final int IN_CHUNK = 10_000;

    private final MongoDatabase db;

    public MongoReferencedRepository(MongoDatabase mongoDatabase) {
        this.db = mongoDatabase;
    }

    private MongoCollection<Document> col(String name) {
        return db.getCollection(name);
    }

    // ---------- insert ----------

    /**
     * baut aus jedem CustomerDocument die normalisierten documents
     * store/film/series katalog wird dedupliziert (ist shared data)
     *
     * katalog wird pro aufruf mitgeschrieben -> nur für einen einzigen
     * aufruf pro datensatz geeignet (BenchmarkRunner). für chunk weises
     * schreiben insertCatalog einmal + insertCustomers pro chunk nutzen
     */
    @Override
    public long insertBatch(List<CustomerDocument> documents, int batchSize) {
        RefDocuments out = new RefDocuments();
        long seq = 0;
        for (CustomerDocument doc : documents) {
            seq = collectCustomer(doc, out, true, seq);
        }
        out.insert(batchSize);
        return documents.size();
    }

    /**
     * nur der shared katalog: stores, films, series + join collections
     * (einmal pro datensatz, vor insertCustomers)
     */
    public void insertCatalog(Collection<CustomerDocument.StoreJson> stores, int batchSize) {
        RefDocuments out = new RefDocuments();
        for (CustomerDocument.StoreJson store : stores) {
            addStore(store, out, 0);
        }
        out.insert(batchSize);
    }

    /**
     * nur die kunden seite: customers, addresses, billings, placements, orders
     * katalog (stores/films/series) muss schon per insertCatalog existieren
     */
    public long insertCustomers(List<CustomerDocument> documents, int batchSize) {
        RefDocuments out = new RefDocuments();
        for (CustomerDocument doc : documents) {
            collectCustomer(doc, out, false, 0);
        }
        out.insert(batchSize);
        return documents.size();
    }

    /** gesammelte documents pro collection, insert reihenfolge wie bisher */
    private final class RefDocuments {
        final Map<Long, Document> customers = new LinkedHashMap<>();
        final Map<Long, Document> addresses = new LinkedHashMap<>();
        final Map<Long, Document> billings = new LinkedHashMap<>();
        final Map<Long, Document> placements = new LinkedHashMap<>();
        final Map<Long, Document> orders = new LinkedHashMap<>();
        // shared katalog, dedupliziert
        final Map<Long, Document> stores = new LinkedHashMap<>();
        final Map<Long, Document> films = new LinkedHashMap<>();
        final Map<Long, Document> series = new LinkedHashMap<>();
        final Map<String, Document> storeFilms = new LinkedHashMap<>();
        final Map<String, Document> storeSeries = new LinkedHashMap<>();

        void insert(int batchSize) {
            insertChunked(CUSTOMERS, customers.values(), batchSize);
            insertChunked(ADDRESSES, addresses.values(), batchSize);
            insertChunked(BILLINGS, billings.values(), batchSize);
            insertChunked(PLACEMENTS, placements.values(), batchSize);
            insertChunked(ORDERS, orders.values(), batchSize);
            insertChunked(STORES, stores.values(), batchSize);
            insertChunked(FILMS, films.values(), batchSize);
            insertChunked(SERIES, series.values(), batchSize);
            insertChunked(STORE_FILMS, storeFilms.values(), batchSize);
            insertChunked(STORE_SERIES, storeSeries.values(), batchSize);
        }
    }

    /** ein CustomerDocument -> normalisierte documents, gibt neuen seq wert zurück */
    private long collectCustomer(CustomerDocument doc, RefDocuments out, boolean withCatalog, long seq) {
        Long cid = doc.id();
        out.customers.put(cid, new Document("_id", cid)
                .append("nickname", doc.nickname())
                .append("email", doc.email())
                .append("firstName", doc.firstName())
                .append("lastName", doc.lastName())
                .append("permission", doc.permission())
                .append("addressId", doc.address() != null ? cid : null));

        if (doc.address() != null) {
            CustomerDocument.AddressJson a = doc.address();
            out.addresses.put(cid, new Document("_id", cid)
                    .append("street", a.street())
                    .append("city", a.city())
                    .append("zipCode", a.zipCode())
                    .append("country", a.country()));
        }

        if (doc.billingInformation() != null) {
            int i = 0;
            for (CustomerDocument.BillingJson b : doc.billingInformation()) {
                long bid = cid * 1000 + i++;
                out.billings.put(bid, new Document("_id", bid)
                        .append("customerId", cid)
                        .append("cardNumber", b.cardNumber())
                        .append("expirationDate", b.expirationDate())
                        .append("cvv", b.cvv()));
            }
        }

        if (doc.orderPlacements() != null) {
            int i = 0;
            for (CustomerDocument.PlacementJson p : doc.orderPlacements()) {
                long pid = cid * 1000 + i++;
                CustomerDocument.OrderJson o = p.order();
                out.placements.put(pid, new Document("_id", pid)
                        .append("customerId", cid)
                        .append("orderId", o != null ? o.orderId() : null)
                        .append("orderDate", p.orderDate() != null ? p.orderDate().toString() : null)
                        .append("orderReturnDate", p.orderReturnDate() != null ? p.orderReturnDate().toString() : null)
                        .append("actualReturnDate", p.actualReturnDate() != null ? p.actualReturnDate().toString() : null));

                if (o != null) {
                    out.orders.put(o.orderId(), new Document("_id", o.orderId())
                            .append("storeId", o.store() != null ? o.store().storeId() : null)
                            .append("orderDate", o.orderDate() != null ? o.orderDate().toString() : null)
                            .append("totalAmount", o.totalAmount() != null ? o.totalAmount().doubleValue() : null));

                    if (withCatalog && o.store() != null) {
                        addStore(o.store(), out, seq++);
                    }
                }
            }
        }
        return seq;
    }

    private void addStore(CustomerDocument.StoreJson s, RefDocuments out, long seqBase) {
        out.stores.putIfAbsent(s.storeId(), new Document("_id", s.storeId())
                .append("phoneNumber", s.phoneNumber()));
        addCatalog(s, out.films, out.series, out.storeFilms, out.storeSeries, seqBase);
    }

    private void addCatalog(CustomerDocument.StoreJson store,
                            Map<Long, Document> films,
                            Map<Long, Document> series,
                            Map<String, Document> storeFilms,
                            Map<String, Document> storeSeries,
                            long seqBase) {
        long storeId = store.storeId();
        if (store.films() != null) {
            int i = 0;
            for (CustomerDocument.FilmJson f : store.films()) {
                films.putIfAbsent(f.filmId(), filmDoc(f));
                storeFilms.putIfAbsent(storeId + ":" + f.filmId(),
                        new Document("_id", storeId * 100_000 + i + seqBase)
                                .append("storeId", storeId)
                                .append("filmId", f.filmId()));
                i++;
            }
        }
        if (store.series() != null) {
            int i = 0;
            for (CustomerDocument.SeriesJson s : store.series()) {
                series.putIfAbsent(s.seriesId(), seriesDoc(s));
                storeSeries.putIfAbsent(storeId + ":" + s.seriesId(),
                        new Document("_id", storeId * 100_000 + 50_000 + i + seqBase)
                                .append("storeId", storeId)
                                .append("seriesId", s.seriesId()));
                i++;
            }
        }
    }

    private static Document filmDoc(CustomerDocument.FilmJson f) {
        return new Document("_id", f.filmId())
                .append("title", f.title())
                .append("releaseYear", f.releaseYear())
                .append("length", f.length())
                .append("languages", f.languages() != null ? f.languages() : List.of())
                .append("genres", f.genres() != null ? f.genres() : List.of());
    }

    private static Document seriesDoc(CustomerDocument.SeriesJson s) {
        return new Document("_id", s.seriesId())
                .append("title", s.title())
                .append("releaseYear", s.releaseYear())
                .append("episodes", s.episodes())
                .append("length", s.length())
                .append("languages", s.languages() != null ? s.languages() : List.of())
                .append("genres", s.genres() != null ? s.genres() : List.of());
    }

    private void insertChunked(String collection, Iterable<Document> docs, int batchSize) {
        List<Document> batch = new ArrayList<>(batchSize);
        for (Document d : docs) {
            batch.add(d);
            if (batch.size() >= batchSize) {
                col(collection).insertMany(batch);
                batch.clear();
            }
        }
        if (!batch.isEmpty()) {
            col(collection).insertMany(batch);
        }
    }

    // ---------- finds ----------

    @Override
    public List<CustomerDocument> findAll(int limit) {
        List<Document> raw = col(CUSTOMERS).find()
                .limit(limit)
                .into(new ArrayList<>());
        return hydrate(raw);
    }

    @Override
    public List<CustomerDocument> findByCity(String city, int limit) {
        // $lookup auf addresses, dann match auf city, dann hydrate rest
        List<Bson> pipeline = List.of(
                Aggregates.lookup(ADDRESSES, "addressId", "_id", "address"),
                Aggregates.unwind("$address", new UnwindOptions().preserveNullAndEmptyArrays(true)),
                Aggregates.match(Filters.eq("address.city", city)),
                Aggregates.limit(limit));
        List<Document> raw = col(CUSTOMERS).aggregate(pipeline).into(new ArrayList<>());
        return hydrate(raw);
    }

    /**
     * projektion: $lookup address + $project, nur id/nickname/city
     */
    @Override
    public List<CustomerProjection> findByCityProjection(String city, int limit) {
        return projectionPipeline(city, false, true, limit);
    }

    @Override
    public List<CustomerProjection> findByCityProjectionSorted(String city, boolean ascending, int limit) {
        return projectionPipeline(city, true, ascending, limit);
    }

    private List<CustomerProjection> projectionPipeline(String city, boolean sorted, boolean ascending, int limit) {
        List<Bson> stages = new ArrayList<>(List.of(
                Aggregates.lookup(ADDRESSES, "addressId", "_id", "address"),
                Aggregates.unwind("$address", new UnwindOptions().preserveNullAndEmptyArrays(true)),
                Aggregates.match(Filters.eq("address.city", city)),
                Aggregates.project(Projections.include("_id", "nickname", "address.city"))));
        if (sorted) {
            stages.add(Aggregates.sort(ascending ? Sorts.ascending("nickname") : Sorts.descending("nickname")));
        }
        stages.add(Aggregates.limit(limit));

        List<Document> rows = col(CUSTOMERS).aggregate(stages).into(new ArrayList<>());
        List<CustomerProjection> out = new ArrayList<>(rows.size());
        for (Document row : rows) {
            Object id = row.get("_id");
            Document address = row.get("address", Document.class);
            out.add(new CustomerProjection(
                    id instanceof Number n ? n.longValue() : null,
                    row.getString("nickname"),
                    address != null ? address.getString("city") : null));
        }
        return out;
    }

    // ---------- updates / delete ----------

    @Override
    public long updateLastNameByNickname(String nickname, String lastName) {
        return col(CUSTOMERS).updateOne(
                Filters.eq("nickname", nickname),
                new Document("$set", new Document("lastName", lastName))).getModifiedCount();
    }

    @Override
    public long updateLastNameAll(String lastName) {
        return col(CUSTOMERS).updateMany(
                new Document(),
                new Document("$set", new Document("lastName", lastName))).getModifiedCount();
    }

    /**
     * delete mit filter auf stadt: customer + alle kind datensaetze
     * (billings, placements, orders, addresses) werden mit geloescht
     */
    @Override
    public long deleteByCity(String city) {
        // address ids der stadt
        Set<Long> addressIds = new HashSet<>();
        for (Document a : col(ADDRESSES).find(Filters.eq("city", city))
                .projection(Projections.include("_id")).into(new ArrayList<>())) {
            Long id = lng(a, "_id");
            if (id != null) addressIds.add(id);
        }
        if (addressIds.isEmpty()) return 0;

        // customer ids (addressId == customer id in unserem schema)
        Set<Long> customerIds = new HashSet<>(addressIds);

        // order ids ueber die placements, bevor die geloescht werden
        Set<Long> orderIds = new HashSet<>();
        forEachChunked(customerIds, chunk -> {
            for (Document p : col(PLACEMENTS)
                    .find(Filters.in("customerId", chunk))
                    .projection(Projections.include("orderId")).into(new ArrayList<>())) {
                Long oid = lng(p, "orderId");
                if (oid != null) orderIds.add(oid);
            }
        });

        long deleted = 0;
        deleted += deleteInChunks(CUSTOMERS, "_id", customerIds);
        deleted += deleteInChunks(BILLINGS, "customerId", customerIds);
        deleted += deleteInChunks(PLACEMENTS, "customerId", customerIds);
        deleted += deleteInChunks(ORDERS, "_id", orderIds);
        deleteInChunks(ADDRESSES, "_id", addressIds);
        return deleted;
    }

    // ---------- aggregation ----------

    /**
     * summe order totals gruppiert nach stadt - echte $lookup kette:
     * placements -> orders (totalAmount) -> customers -> addresses (city)
     */
    @Override
    public Map<String, BigDecimal> sumOrderTotalsByCity() {
        List<Bson> pipeline = List.of(
                Aggregates.lookup(ORDERS, "orderId", "_id", "order"),
                Aggregates.unwind("$order", new UnwindOptions().preserveNullAndEmptyArrays(true)),
                Aggregates.lookup(CUSTOMERS, "customerId", "_id", "customer"),
                Aggregates.unwind("$customer", new UnwindOptions().preserveNullAndEmptyArrays(true)),
                Aggregates.lookup(ADDRESSES, "customer.addressId", "_id", "address"),
                Aggregates.unwind("$address", new UnwindOptions().preserveNullAndEmptyArrays(true)),
                Aggregates.group("$address.city",
                        Accumulators.sum("total", "$order.totalAmount")));
        List<Document> rows = col(PLACEMENTS).aggregate(pipeline).into(new ArrayList<>());
        Map<String, BigDecimal> result = new LinkedHashMap<>();
        for (Document row : rows) {
            String city = row.getString("_id");
            Object total = row.get("total");
            // totalAmount ist als string gespeichert -> $sum gibt double
            BigDecimal sum = total instanceof Number n
                    ? BigDecimal.valueOf(n.doubleValue())
                    : new BigDecimal(String.valueOf(total));
            result.put(city, sum.setScale(2, java.math.RoundingMode.HALF_UP));
        }
        return result;
    }

    // ---------- hydration: refs -> volles CustomerDocument ----------

    /**
     * baut aus rohen customer docs die vollen CustomerDocuments,
     * alle referenzierten daten werden gebuendelt nachgeladen ($in)
     */
    private List<CustomerDocument> hydrate(List<Document> rawCustomers) {
        if (rawCustomers.isEmpty()) return List.of();

        List<Long> customerIds = new ArrayList<>(rawCustomers.size());
        Set<Long> addressIds = new HashSet<>();
        for (Document c : rawCustomers) {
            Long id = lng(c, "_id");
            customerIds.add(id);
            Long addrId = lng(c, "addressId");
            if (addrId != null) addressIds.add(addrId);
        }

        Map<Long, Document> addressMap = fetchByIds(ADDRESSES, addressIds);
        Map<Long, List<Document>> billingsByCustomer = fetchChildren(BILLINGS, "customerId", customerIds);
        Map<Long, List<Document>> placementsByCustomer = fetchChildren(PLACEMENTS, "customerId", customerIds);

        // orders + stores + katalog
        Set<Long> orderIds = new HashSet<>();
        for (List<Document> ps : placementsByCustomer.values()) {
            for (Document p : ps) {
                Long oid = lng(p, "orderId");
                if (oid != null) orderIds.add(oid);
            }
        }
        Map<Long, Document> orderMap = fetchByIds(ORDERS, orderIds);

        Set<Long> storeIds = new HashSet<>();
        for (Document o : orderMap.values()) {
            Long sid = lng(o, "storeId");
            if (sid != null) storeIds.add(sid);
        }
        Map<Long, Document> storeMap = fetchByIds(STORES, storeIds);

        // m:n katalog: storeId -> filmIds/seriesIds -> docs
        Map<Long, List<Long>> filmsByStore = fetchJoinIds(STORE_FILMS, "storeId", "filmId", storeIds);
        Map<Long, List<Long>> seriesByStore = fetchJoinIds(STORE_SERIES, "storeId", "seriesId", storeIds);

        Set<Long> filmIds = new HashSet<>();
        Set<Long> seriesIds = new HashSet<>();
        filmsByStore.values().forEach(filmIds::addAll);
        seriesByStore.values().forEach(seriesIds::addAll);
        Map<Long, Document> filmMap = fetchByIds(FILMS, filmIds);
        Map<Long, Document> seriesMap = fetchByIds(SERIES, seriesIds);

        // storeId -> fertiges StoreJson (cache, shared stores)
        Map<Long, CustomerDocument.StoreJson> storeCache = new HashMap<>();

        List<CustomerDocument> out = new ArrayList<>(rawCustomers.size());
        for (Document c : rawCustomers) {
            Long cid = lng(c, "_id");
            CustomerDocument.AddressJson address = null;
            Document addrDoc = addressMap.get(lng(c, "addressId"));
            if (addrDoc != null) {
                address = new CustomerDocument.AddressJson(
                        str(addrDoc, "street"), str(addrDoc, "city"),
                        str(addrDoc, "zipCode"), str(addrDoc, "country"));
            }

            List<CustomerDocument.BillingJson> billings = new ArrayList<>();
            for (Document b : billingsByCustomer.getOrDefault(cid, List.of())) {
                billings.add(new CustomerDocument.BillingJson(
                        str(b, "cardNumber"), str(b, "expirationDate"),
                        (int) (lng(b, "cvv") != null ? lng(b, "cvv") : 0)));
            }

            List<CustomerDocument.PlacementJson> placements = new ArrayList<>();
            for (Document p : placementsByCustomer.getOrDefault(cid, List.of())) {
                CustomerDocument.OrderJson orderJson = null;
                Long oid = lng(p, "orderId");
                Document orderDoc = oid != null ? orderMap.get(oid) : null;
                if (orderDoc != null) {
                    Long sid = lng(orderDoc, "storeId");
                    CustomerDocument.StoreJson storeJson =
                            sid != null ? storeJson(sid, storeMap, filmsByStore, seriesByStore,
                                    filmMap, seriesMap, storeCache) : null;
                    orderJson = new CustomerDocument.OrderJson(
                            oid,
                            dt(orderDoc, "orderDate"),
                            dec(orderDoc, "totalAmount"),
                            storeJson);
                }
                placements.add(new CustomerDocument.PlacementJson(
                        orderJson,
                        date(p, "orderDate"),
                        date(p, "orderReturnDate"),
                        date(p, "actualReturnDate")));
            }

            out.add(new CustomerDocument(
                    cid,
                    str(c, "nickname"), str(c, "email"),
                    str(c, "firstName"), str(c, "lastName"),
                    str(c, "permission"),
                    address, billings, placements));
        }
        return out;
    }

    private CustomerDocument.StoreJson storeJson(Long storeId,
                                                 Map<Long, Document> storeMap,
                                                 Map<Long, List<Long>> filmsByStore,
                                                 Map<Long, List<Long>> seriesByStore,
                                                 Map<Long, Document> filmMap,
                                                 Map<Long, Document> seriesMap,
                                                 Map<Long, CustomerDocument.StoreJson> cache) {
        return cache.computeIfAbsent(storeId, sid -> {
            Document storeDoc = storeMap.get(sid);
            String phone = storeDoc != null ? str(storeDoc, "phoneNumber") : null;
            List<CustomerDocument.FilmJson> films = new ArrayList<>();
            for (Long fid : filmsByStore.getOrDefault(sid, List.of())) {
                Document f = filmMap.get(fid);
                if (f != null) {
                    films.add(new CustomerDocument.FilmJson(
                            fid, str(f, "title"), (Integer) numOrNull(f.get("releaseYear")),
                            f.get("length") instanceof Number n ? n.floatValue() : null,
                            strList(f, "languages"), strList(f, "genres")));
                }
            }
            List<CustomerDocument.SeriesJson> series = new ArrayList<>();
            for (Long sid2 : seriesByStore.getOrDefault(sid, List.of())) {
                Document s = seriesMap.get(sid2);
                if (s != null) {
                    series.add(new CustomerDocument.SeriesJson(
                            sid2, str(s, "title"), (Integer) numOrNull(s.get("releaseYear")),
                            (Integer) numOrNull(s.get("episodes")),
                            s.get("length") instanceof Number n ? n.floatValue() : null,
                            strList(s, "languages"), strList(s, "genres")));
                }
            }
            return new CustomerDocument.StoreJson(sid, phone, films, series);
        });
    }

    // ---------- fetch helpers ($in in chunks) ----------

    /** docs per _id, {$in: ids} in chunks */
    private Map<Long, Document> fetchByIds(String collection, Set<Long> ids) {
        Map<Long, Document> out = new HashMap<>();
        if (ids.isEmpty()) return out;
        forEachChunked(ids, chunk -> {
            for (Document d : col(collection).find(Filters.in("_id", chunk)).into(new ArrayList<>())) {
                Long id = lng(d, "_id");
                if (id != null) out.put(id, d);
            }
        });
        return out;
    }

    /** kind docs gruppiert nach foreign key */
    private Map<Long, List<Document>> fetchChildren(String collection, String fkField, List<Long> parentIds) {
        Map<Long, List<Document>> out = new HashMap<>();
        if (parentIds.isEmpty()) return out;
        forEachChunked(new HashSet<>(parentIds), chunk -> {
            for (Document d : col(collection).find(Filters.in(fkField, chunk)).into(new ArrayList<>())) {
                Long fk = lng(d, fkField);
                if (fk != null) out.computeIfAbsent(fk, k -> new ArrayList<>()).add(d);
            }
        });
        return out;
    }

    /** join collection: parentId -> liste von kind ids */
    private Map<Long, List<Long>> fetchJoinIds(String collection, String parentField, String childField, Set<Long> parentIds) {
        Map<Long, List<Long>> out = new HashMap<>();
        if (parentIds.isEmpty()) return out;
        forEachChunked(parentIds, chunk -> {
            for (Document d : col(collection).find(Filters.in(parentField, chunk))
                    .projection(Projections.include(parentField, childField)).into(new ArrayList<>())) {
                Long pid = lng(d, parentField);
                Long cid = lng(d, childField);
                if (pid != null && cid != null) {
                    out.computeIfAbsent(pid, k -> new ArrayList<>()).add(cid);
                }
            }
        });
        return out;
    }

    private void forEachChunked(Set<Long> ids, java.util.function.Consumer<List<Long>> consumer) {
        List<Long> all = new ArrayList<>(ids);
        for (int i = 0; i < all.size(); i += IN_CHUNK) {
            consumer.accept(all.subList(i, Math.min(i + IN_CHUNK, all.size())));
        }
    }

    private long deleteInChunks(String collection, String field, Set<Long> ids) {
        long[] total = {0};
        forEachChunked(ids, chunk ->
                total[0] += col(collection).deleteMany(Filters.in(field, chunk)).getDeletedCount());
        return total[0];
    }

    // ---------- bson -> java helpers ----------

    private static String str(Document d, String key) {
        Object v = d.get(key);
        return v != null ? v.toString() : null;
    }

    private static Long lng(Document d, String key) {
        Object v = d.get(key);
        return v instanceof Number n ? n.longValue() : null;
    }

    private static Object numOrNull(Object v) {
        return v instanceof Number ? ((Number) v).intValue() : null;
    }

    private static LocalDate date(Document d, String key) {
        String s = str(d, key);
        return s != null && !s.isEmpty() ? LocalDate.parse(s) : null;
    }

    private static LocalDateTime dt(Document d, String key) {
        String s = str(d, key);
        return s != null && !s.isEmpty() ? LocalDateTime.parse(s) : null;
    }

    private static BigDecimal dec(Document d, String key) {
        Object v = d.get(key);
        if (v instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
        String s = v != null ? v.toString() : null;
        return s != null && !s.isEmpty() ? new BigDecimal(s) : null;
    }

    @SuppressWarnings("unchecked")
    private static List<String> strList(Document d, String key) {
        Object v = d.get(key);
        if (v instanceof List<?> list) {
            return list.stream().map(String::valueOf).toList();
        }
        return List.of();
    }
}
