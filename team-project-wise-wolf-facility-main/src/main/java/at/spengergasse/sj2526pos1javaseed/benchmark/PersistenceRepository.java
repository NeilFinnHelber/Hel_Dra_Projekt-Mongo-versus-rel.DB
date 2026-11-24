package at.spengergasse.sj2526pos1javaseed.benchmark;

import at.spengergasse.sj2526pos1javaseed.nosql.model.CustomerDocument;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * gemeinsames interface für den vergleich
 * relationale db (jpa) vs mongo
 *
 * - beide varianten machen exakt die selben operationen
 *   (benchmark.jpa + nosql)
 * - datensatz ist überall CustomerDocument
 *   also "frontend optimiertes" dok mit embedded stuff
 */
public interface PersistenceRepository {

    /** bulk insert, gibt anzahl geschriebener datensätze zurück */
    long insertBatch(List<CustomerDocument> documents, int batchSize);

    /** find 1: ohne filter */
    List<CustomerDocument> findAll(int limit);

    /** find 2: filter auf stadt */
    List<CustomerDocument> findByCity(String city, int limit);

    /** find 3: filter + projektion (nur id, nickname, city) */
    List<CustomerProjection> findByCityProjection(String city, int limit);

    /** find 4: filter + projektion + sortierung nach nickname */
    List<CustomerProjection> findByCityProjectionSorted(String city, boolean ascending, int limit);

    /** update mit filter auf nickname */
    long updateLastNameByNickname(String nickname, String lastName);

    /** update ohne filter, alle datensätze */
    long updateLastNameAll(String lastName);

    /** delete mit filter auf stadt */
    long deleteByCity(String city);

    /**
     * aggregation: summe der order totals gruppiert nach stadt
     * mongo: $unwind orderPlacements + $group (sum totalAmount) by address.city
     * jpa:  group by op.customer.address.city, sum(op.order.totalAmount)
     */
    Map<String, BigDecimal> sumOrderTotalsByCity();
}
