package at.spengergasse.sj2526pos1javaseed.benchmark.write;

import at.spengergasse.sj2526pos1javaseed.nosql.ref.MongoReferencedRepository;
import at.spengergasse.sj2526pos1javaseed.testdata.RelationalTestDataWriter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/** config validierung, statistik, csv/json format, datensatz */
class BenchmarkCoreTest {

    private static BenchmarkMeasurement m(long id, String db, int scale, int rep, boolean warmup, double ms) {
        return BenchmarkMeasurement.of(id, "insert", db, scale, rep, warmup, (long) (ms * 1_000_000), scale,
                Instant.parse("2026-11-28T10:00:00Z"));
    }

    // ---------- config ----------

    @Test
    void config_defaults_match_issue_8() {
        BenchmarkConfig c = BenchmarkConfig.defaults();
        assertThat(c.databases()).containsExactly("rel", "mongo", "mongo-ref");
        assertThat(c.scales()).containsExactly(100, 1000, 100_000);
        assertThat(c.repetitions()).isEqualTo(5);
        assertThat(c.warmups()).isEqualTo(1);
        assertThat(c.seed()).isEqualTo(42L);
        assertThat(c.chunkSize()).isEqualTo(1000);
    }

    @Test
    void config_normalizes_and_validates() {
        BenchmarkConfig c = new BenchmarkConfig(List.of("REL", "mongo", "rel"), List.of(100, 100, 1000),
                null, null, null, null);
        assertThat(c.databases()).containsExactly("rel", "mongo");
        assertThat(c.scales()).containsExactly(100, 1000);
        assertThat(c.includesRelational()).isTrue();
        assertThat(c.includesMongo()).isTrue();
        assertThat(new BenchmarkConfig(List.of("mongo-ref"), List.of(10), 1, 0, 1L, 1)
                .includesRelational()).isFalse();
    }

    @Test
    void config_rejects_invalid_values() {
        assertThatThrownBy(() -> new BenchmarkConfig(List.of("oracle"), null, null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("oracle");
        assertThatThrownBy(() -> new BenchmarkConfig(List.of(" "), null, null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new BenchmarkConfig(null, List.of(5), null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new BenchmarkConfig(null, List.of(200_000), null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new BenchmarkConfig(null, null, 0, null, null, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new BenchmarkConfig(null, null, null, -1, null, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new BenchmarkConfig(null, null, null, null, null, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ---------- statistik ----------

    @Test
    void summary_excludes_warmups_and_computes_statistics() {
        List<BenchmarkSummary> summaries = BenchmarkSummary.of(List.of(
                m(1, "rel", 100, 1, true, 999),
                m(2, "rel", 100, 1, false, 10),
                m(3, "rel", 100, 2, false, 20),
                m(4, "rel", 100, 3, false, 30),
                m(5, "rel", 100, 4, false, 40),
                m(6, "mongo", 100, 1, false, 5)));

        assertThat(summaries).hasSize(2);
        BenchmarkSummary rel = summaries.getFirst();
        assertThat(rel.database()).isEqualTo("rel");
        assertThat(rel.n()).isEqualTo(4);
        assertThat(rel.meanMs()).isCloseTo(25, within(1e-9));
        assertThat(rel.medianMs()).isCloseTo(25, within(1e-9));
        assertThat(rel.minMs()).isCloseTo(10, within(1e-9));
        assertThat(rel.maxMs()).isCloseTo(40, within(1e-9));
        // stichprobe: sqrt(((15²+5²)*2)/3)
        assertThat(rel.stddevMs()).isCloseTo(Math.sqrt(500.0 / 3), within(1e-9));
        assertThat(rel.recordsPerSecond()).isCloseTo(100 / 0.025, within(1e-6));

        BenchmarkSummary mongo = summaries.get(1);
        assertThat(mongo.n()).isEqualTo(1);
        assertThat(mongo.medianMs()).isCloseTo(5, within(1e-9));
        assertThat(mongo.stddevMs()).isZero();
    }

    @Test
    void summary_median_odd_count() {
        List<BenchmarkSummary> s = BenchmarkSummary.of(List.of(
                m(1, "rel", 10, 1, false, 30), m(2, "rel", 10, 2, false, 10), m(3, "rel", 10, 3, false, 100)));
        assertThat(s.getFirst().medianMs()).isCloseTo(30, within(1e-9));
    }

    // ---------- csv / json ----------

    private static BenchmarkReport report() {
        BenchmarkReport report = new BenchmarkReport(7,
                new BenchmarkConfig(List.of("rel", "mongo"), List.of(100), 2, 1, 42L, 1000));
        report.setEnvironment(Map.of("mongoWriteConcernClient", "WriteConcern{w=null}"));
        report.add(m(1, "rel", 100, 1, true, 12.3456));
        report.add(m(2, "rel", 100, 1, false, 10.5));
        report.add(m(3, "mongo", 100, 1, false, 7.25));
        report.finish();
        return report;
    }

    @Test
    void csv_keeps_k6_columns_and_every_measurement() {
        List<String> lines = BenchmarkCsv.measurementLines(report());

        assertThat(lines.getFirst())
                .startsWith("id,database,scale,indexVariant,status,operation,durationMs")
                .isEqualTo("id,database,scale,indexVariant,status,operation,durationMs,repetition,warmup,records");
        assertThat(lines).hasSize(4);
        assertThat(lines.get(1)).isEqualTo("1,rel,100,false,FINISHED,insert,12.346,1,true,100");
        assertThat(lines.get(3)).isEqualTo("3,mongo,100,false,FINISHED,insert,7.250,1,false,100");
        assertThat(BenchmarkCsv.measurements(report())).endsWith("1,false,100\n").contains("\n1,rel,");
    }

    @Test
    void summary_csv_has_one_row_per_combination() {
        List<String> lines = BenchmarkCsv.summaryLines(report());

        assertThat(lines.getFirst()).isEqualTo(BenchmarkCsv.SUMMARY_HEADER);
        assertThat(lines).hasSize(3);
        assertThat(lines.get(1)).startsWith("insert,rel,100,1,10.500,10.500,10.500,10.500,0.000,100,");
    }

    @Test
    void report_serializes_to_json_like_spring_does() throws Exception {
        // wie spring boot: java time modul, datum als iso string
        ObjectMapper mapper = JsonMapper.builder().findAndAddModules()
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS).build();
        JsonNode json = mapper.readTree(mapper.writeValueAsString(report()));

        assertThat(json.get("id").asLong()).isEqualTo(7);
        assertThat(json.get("operation").asText()).isEqualTo("insert");
        assertThat(json.get("status").asText()).isEqualTo("FINISHED");
        assertThat(json.get("config").get("databases")).hasSize(2);
        assertThat(json.get("config").get("repetitions").asInt()).isEqualTo(2);
        assertThat(json.get("measurements")).hasSize(3);
        assertThat(json.get("measurements").get(0).get("durationMs").asDouble()).isCloseTo(12.3456, within(1e-6));
        assertThat(json.get("summaries")).hasSize(2);
        assertThat(json.get("environment").get("mongoWriteConcernClient").asText()).contains("WriteConcern");
        assertThat(json.get("startedAt").isTextual()).isTrue();
        assertThat(json.get("totalSteps").asInt()).isEqualTo(6);
        assertThat(json.get("completedSteps").asInt()).isEqualTo(3);
    }

    // ---------- datensatz ----------

    @Test
    void dataset_is_deterministic_and_counts_are_consistent() {
        BenchmarkDataset a = BenchmarkDataset.generate(42, 100, 30);
        BenchmarkDataset b = BenchmarkDataset.generate(42, 100, 1000);

        assertThat(a.customers()).isEqualTo(b.customers());
        assertThat(a.documents()).hasSize(100);
        assertThat(a.stores()).hasSize(a.catalog().stores().size())
                .extracting(s -> s.storeId()).isSorted();
        assertThat(a.expectedRelationalRows()).containsEntry("customer", 100L)
                .containsEntry("orders", a.placementCount())
                .containsEntry("order_placement", a.placementCount())
                .containsEntry("billing_information", a.billingCount())
                .containsKeys(RelationalTestDataWriter.TABLES.toArray(String[]::new));
        assertThat(a.expectedReferencedCounts())
                .containsKeys(MongoReferencedRepository.ALL_COLLECTIONS.toArray(String[]::new))
                .containsEntry(MongoReferencedRepository.ORDERS, a.placementCount());
    }

    @Test
    void chunks_cover_all_items_in_order() {
        List<List<Integer>> chunks = BenchmarkDataset.chunks(List.of(1, 2, 3, 4, 5, 6, 7), 3);
        assertThat(chunks).containsExactly(List.of(1, 2, 3), List.of(4, 5, 6), List.of(7));
    }
}
