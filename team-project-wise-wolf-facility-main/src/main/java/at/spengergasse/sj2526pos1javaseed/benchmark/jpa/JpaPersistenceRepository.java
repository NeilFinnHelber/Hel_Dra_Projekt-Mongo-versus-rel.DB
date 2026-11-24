package at.spengergasse.sj2526pos1javaseed.benchmark.jpa;

import at.spengergasse.sj2526pos1javaseed.Domain.*;
import at.spengergasse.sj2526pos1javaseed.Ids.*;
import at.spengergasse.sj2526pos1javaseed.benchmark.CustomerProjection;
import at.spengergasse.sj2526pos1javaseed.benchmark.PersistenceRepository;
import at.spengergasse.sj2526pos1javaseed.nosql.model.CustomerDocument;
import at.spengergasse.sj2526pos1javaseed.richtypes.Email;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * relationale implementierung vom benchmark interface (jpa/hbm)
 *
 * - gleicher CustomerDocument datensatz wie bei mongo
 * - normalisierte tabellen werden beim lesen wieder zum doc
 *   zusammengeführt (customer + billing + placements + orders + store)
 * - genau der overhead den der vergleich zeigen soll:
 *   mongo = 1 doc fetch, hier = mehrere queries + joins
 * - writes laufen gebatched (flush/clear pro batchSize)
 *   damit der persistenzkontext nicht vollläuft
 */
@Repository
public class JpaPersistenceRepository implements PersistenceRepository {

    @PersistenceContext
    private EntityManager entityManager;

    // ---------- Writes ----------

    @Override
    @Transactional
    public long insertBatch(List<CustomerDocument> documents, int batchSize) {
        int i = 0;
        for (CustomerDocument doc : documents) {
            persistAggregate(doc);
            if (++i % batchSize == 0) {
                entityManager.flush();
                entityManager.clear();
            }
        }
        entityManager.flush();
        entityManager.clear();
        return documents.size();
    }

    private void persistAggregate(CustomerDocument doc) {
        Customer customer = Customer.builder()
                .id(new UserId(doc.id()))
                .nickname(doc.nickname())
                .firstName(doc.firstName())
                .lastName(doc.lastName())
                .email(doc.email() != null ? new Email(doc.email()) : null)
                .address(toAddress(doc.address()))
                .permission(doc.permission() != null ? Permission.valueOf(doc.permission()) : Permission.Customer)
                .build();
        entityManager.persist(customer);

        if (doc.billingInformation() != null) {
            int idx = 0;
            for (CustomerDocument.BillingJson b : doc.billingInformation()) {
                BillingInformation billing = BillingInformation.builder()
                        .id(new BillingInformationId(doc.id() * 1_000_000L + idx++))
                        .cardNumber(b.cardNumber())
                        .expirationDate(b.expirationDate() != null ? LocalDate.parse(b.expirationDate()) : LocalDate.now())
                        .cvv(b.cvv())
                        .customer(customer)
                        .build();
                entityManager.persist(billing);
            }
        }

        if (doc.orderPlacements() != null) {
            int idx = 0;
            for (CustomerDocument.PlacementJson p : doc.orderPlacements()) {
                Order order = ensureOrder(p.orderId(), p.storeId(), p.totalAmount());
                Order_Placement placement = Order_Placement.builder()
                        .id(new Order_PlacementId(doc.id() * 1_000_000L + idx++))
                        .orderDate(p.orderDate())
                        .orderReturnDate(p.orderReturnDate())
                        .actualReturnDate(p.actualReturnDate())
                        .customer(customer)
                        .order(order)
                        .build();
                entityManager.persist(placement);
            }
        }
    }

    // stellt sicher dass order/store zeilen existieren (fk pflicht)
    // totalAmount kommt aus dem placement, sonst bleibt order summen auf 0
    private Order ensureOrder(Long orderId, Long storeId, BigDecimal totalAmount) {
        if (orderId == null) {
            return null;
        }
        Order order = entityManager.find(Order.class, new OrderId(orderId));
        if (order != null) {
            // order_id ist unique pro placement, also gehört der betrag zu
            // genau diesem placement - alte runs mit betrag 0 werden repariert
            if (totalAmount != null && order.getTotalAmount() != null
                    && order.getTotalAmount().compareTo(totalAmount) != 0) {
                order.setTotalAmount(totalAmount);
            }
            return order;
        }
        Store store = ensureStore(storeId != null ? storeId : 1L);
        order = Order.builder()
                .id(new OrderId(orderId))
                .orderDate(LocalDateTime.now())
                .totalAmount(totalAmount != null ? totalAmount : BigDecimal.ZERO)
                .store(store)
                .build();
        entityManager.persist(order);
        return order;
    }

    private Store ensureStore(Long storeId) {
        Store store = entityManager.find(Store.class, new StoreId(storeId));
        if (store == null) {
            store = Store.builder()
                    .storeId(new StoreId(storeId))
                    .films(new HashSet<>())
                    .series(new HashSet<>())
                    .build();
            entityManager.persist(store);
        }
        return store;
    }

    // ---------- Reads ----------

    @Override
    @Transactional(readOnly = true)
    public List<CustomerDocument> findAll(int limit) {
        List<Customer> customers = entityManager
                .createQuery("select c from Customer c", Customer.class)
                .setMaxResults(limit)
                .getResultList();
        return toDocuments(customers);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CustomerDocument> findByCity(String city, int limit) {
        List<Customer> customers = entityManager
                .createQuery("select c from Customer c where c.address.city = :city", Customer.class)
                .setParameter("city", city)
                .setMaxResults(limit)
                .getResultList();
        return toDocuments(customers);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CustomerProjection> findByCityProjection(String city, int limit) {
        return projectionQuery(city, null, limit);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CustomerProjection> findByCityProjectionSorted(String city, boolean ascending, int limit) {
        return projectionQuery(city, ascending, limit);
    }

    private List<CustomerProjection> projectionQuery(String city, Boolean ascending, int limit) {
        String jpql = "select c.id, c.nickname, c.address.city from Customer c where c.address.city = :city"
                + (ascending != null ? " order by c.nickname " + (ascending ? "asc" : "desc") : "");
        List<Object[]> rows = entityManager.createQuery(jpql, Object[].class)
                .setParameter("city", city)
                .setMaxResults(limit)
                .getResultList();
        return rows.stream()
                .map(r -> new CustomerProjection(
                        r[0] instanceof UserId id ? id.getId() : null,
                        (String) r[1],
                        (String) r[2]))
                .toList();
    }

    // ---------- Update / Delete ----------

    @Override
    @Transactional
    public long updateLastNameByNickname(String nickname, String lastName) {
        int updated = entityManager.createQuery(
                        "update Customer c set c.lastName = :ln where c.nickname = :nick")
                .setParameter("ln", lastName)
                .setParameter("nick", nickname)
                .executeUpdate();
        entityManager.clear();
        return updated;
    }

    @Override
    @Transactional
    public long updateLastNameAll(String lastName) {
        int updated = entityManager.createQuery("update Customer c set c.lastName = :ln")
                .setParameter("ln", lastName)
                .executeUpdate();
        entityManager.clear();
        return updated;
    }

    @Override
    @Transactional
    public long deleteByCity(String city) {
        // fk reihenfolge: abhängige tabellen zuerst (placements, billings)
        entityManager.createQuery(
                        "delete from Order_Placement op where op.customer.address.city = :city")
                .setParameter("city", city)
                .executeUpdate();
        entityManager.createQuery(
                        "delete from BillingInformation b where b.customer.address.city = :city")
                .setParameter("city", city)
                .executeUpdate();
        int deleted = entityManager.createQuery("delete from Customer c where c.address.city = :city")
                .setParameter("city", city)
                .executeUpdate();
        entityManager.clear();
        return deleted;
    }

    // ---------- Aggregation ----------

    @Override
    @Transactional(readOnly = true)
    public Map<String, BigDecimal> sumOrderTotalsByCity() {
        // group by über placement -> customer.city, sum über order.totalAmount
        List<Object[]> rows = entityManager.createQuery(
                        "select op.customer.address.city, sum(op.order.totalAmount) " +
                                "from Order_Placement op " +
                                "group by op.customer.address.city",
                        Object[].class)
                .getResultList();
        Map<String, BigDecimal> result = new LinkedHashMap<>();
        for (Object[] row : rows) {
            String city = (String) row[0];
            BigDecimal sum = row[1] instanceof BigDecimal bd ? bd : BigDecimal.ZERO;
            result.put(city, sum.setScale(2, java.math.RoundingMode.HALF_UP));
        }
        return result;
    }

    // ---------- Entity -> Document mapping ----------

    private List<CustomerDocument> toDocuments(List<Customer> customers) {
        if (customers.isEmpty()) {
            return List.of();
        }
        Map<Long, List<BillingInformation>> billings = loadBillings(customers);
        Map<Long, List<Order_Placement>> placements = loadPlacements(customers);
        return customers.stream()
                .map(c -> toDocument(c,
                        billings.getOrDefault(c.getId().getId(), List.of()),
                        placements.getOrDefault(c.getId().getId(), List.of())))
                .toList();
    }

    private Map<Long, List<BillingInformation>> loadBillings(List<Customer> customers) {
        List<BillingInformation> list = entityManager.createQuery(
                        "select b from BillingInformation b where b.customer in :customers", BillingInformation.class)
                .setParameter("customers", customers)
                .getResultList();
        return list.stream().collect(Collectors.groupingBy(
                b -> b.getCustomer().getId().getId()));
    }

    private Map<Long, List<Order_Placement>> loadPlacements(List<Customer> customers) {
        List<Order_Placement> list = entityManager.createQuery(
                        "select op from Order_Placement op " +
                                "join fetch op.order o join fetch o.store " +
                                "where op.customer in :customers", Order_Placement.class)
                .setParameter("customers", customers)
                .getResultList();
        return list.stream().collect(Collectors.groupingBy(
                op -> op.getCustomer().getId().getId()));
    }

    private CustomerDocument toDocument(Customer c,
                                        List<BillingInformation> billings,
                                        List<Order_Placement> placements) {
        return new CustomerDocument(
                c.getId().getId(),
                c.getNickname(),
                c.getEmail() != null ? c.getEmail().getValue() : null,
                c.getFirstName(),
                c.getLastName(),
                c.getPermission() != null ? c.getPermission().name() : null,
                toAddressJson(c.getAddress()),
                billings.stream()
                        .map(b -> new CustomerDocument.BillingJson(
                                b.getCardNumber(),
                                b.getExpirationDate() != null ? b.getExpirationDate().toString() : null,
                                b.getCvv()))
                        .toList(),
                placements.stream()
                        .map(this::toPlacementJson)
                        .toList());
    }

    private CustomerDocument.PlacementJson toPlacementJson(Order_Placement op) {
        Order order = op.getOrder();
        return new CustomerDocument.PlacementJson(
                order != null ? order.getId().getId() : null,
                op.getOrderDate(),
                op.getOrderReturnDate(),
                op.getActualReturnDate(),
                order != null ? order.getTotalAmount() : null,
                order != null && order.getStore() != null ? order.getStore().getStoreId().getId() : null);
    }

    private CustomerDocument.AddressJson toAddressJson(Address address) {
        if (address == null) {
            return null;
        }
        return new CustomerDocument.AddressJson(
                address.getStreet(), address.getCity(), address.getZipCode(), address.getCountry());
    }

    private Address toAddress(CustomerDocument.AddressJson json) {
        if (json == null) {
            return null;
        }
        return new Address(json.street(), json.city(), json.zipCode(), json.country());
    }
}
