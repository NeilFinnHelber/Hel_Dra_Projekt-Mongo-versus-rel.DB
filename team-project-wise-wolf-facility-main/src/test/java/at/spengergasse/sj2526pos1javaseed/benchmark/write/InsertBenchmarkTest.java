package at.spengergasse.sj2526pos1javaseed.benchmark.write;

import at.spengergasse.sj2526pos1javaseed.nosql.MongoCustomerRepository;
import at.spengergasse.sj2526pos1javaseed.nosql.ref.MongoReferencedRepository;
import at.spengergasse.sj2526pos1javaseed.testdata.TestDataService;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.LongStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * ablauf vom insert benchmark ohne datenbank: die drei db schritte
 * (prepare/execute/verify) werden überschrieben und nur aufgezeichnet
 */
class InsertBenchmarkTest {

    private TestDataService guard;

    @BeforeEach
    void setUp() {
        guard = mock(TestDataService.class);
    }

    /** InsertBenchmark mit aufgezeichneten statt echten db schritten */
    class RecordingBenchmark extends InsertBenchmark {
        final List<String> calls = new ArrayList<>();
        long prepareSleepMs = 0;
        String failOnVerifyDb;

        RecordingBenchmark() {
            super(guard, mock(JdbcTemplate.class), mock(PlatformTransactionManager.class), mock(MongoClient.class),
                    mock(MongoDatabase.class), mock(MongoCustomerRepository.class),
                    mock(MongoReferencedRepository.class));
        }

        @Override void prepare(String db, BenchmarkDataset ds) {
            calls.add("prepare:" + db + ":" + ds.scale());
            try {
                Thread.sleep(prepareSleepMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        @Override long execute(String db, BenchmarkDataset ds) {
            calls.add("execute:" + db + ":" + ds.scale());
            return ds.scale();
        }

        @Override void verify(String db, BenchmarkDataset ds) {
            calls.add("verify:" + db + ":" + ds.scale());
            if (db.equals(failOnVerifyDb)) {
                throw new IllegalStateException("zu wenige datensätze");
            }
        }

        @Override Map<String, String> describeEnvironment(BenchmarkConfig config) {
            return Map.of("mongoWriteConcernClient", "test");
        }

        BenchmarkReport runSync(BenchmarkConfig config) {
            BenchmarkReport report = new BenchmarkReport(1, config);
            run(report);
            return report;
        }
    }

    private static BenchmarkConfig config(List<String> dbs, List<Integer> scales, int reps, int warmups) {
        return new BenchmarkConfig(dbs, scales, reps, warmups, 42L, 1000);
    }

    @Test
    void every_execute_is_preceded_by_prepare_and_followed_by_verify() {
        RecordingBenchmark b = new RecordingBenchmark();
        b.runSync(config(List.of("rel", "mongo"), List.of(10), 2, 1));

        assertThat(b.calls).hasSize(3 * 2 * 3);
        for (int i = 0; i < b.calls.size(); i += 3) {
            String target = b.calls.get(i).substring("prepare:".length());
            assertThat(b.calls.get(i)).startsWith("prepare:");
            assertThat(b.calls.get(i + 1)).isEqualTo("execute:" + target);
            assertThat(b.calls.get(i + 2)).isEqualTo("verify:" + target);
        }
    }

    @Test
    void measures_all_combinations_with_warmups_flagged() {
        BenchmarkReport report = new RecordingBenchmark()
                .runSync(config(List.of("rel", "mongo", "mongo-ref"), List.of(10, 20), 3, 1));

        assertThat(report.getStatus()).isEqualTo(BenchmarkReport.Status.FINISHED);
        // 2 scales x 3 dbs x (1 warmup + 3 reps)
        assertThat(report.getMeasurements()).hasSize(24);
        assertThat(report.getCompletedSteps()).isEqualTo(report.getTotalSteps());
        assertThat(report.getMeasurements()).filteredOn(BenchmarkMeasurement::warmup).hasSize(6)
                .allMatch(m -> m.repetition() == 1);
        assertThat(report.getMeasurements()).filteredOn(m -> !m.warmup())
                .extracting(BenchmarkMeasurement::repetition).containsOnly(1, 2, 3);
        assertThat(report.getMeasurements()).extracting(BenchmarkMeasurement::id)
                .containsExactlyElementsOf(LongStream.rangeClosed(1, 24).boxed().toList());
        assertThat(report.getMeasurements()).extracting(BenchmarkMeasurement::operation).containsOnly("insert");
        // statistik: 2 scales x 3 dbs, jeweils n = 3 (ohne warmup)
        assertThat(report.getSummaries()).hasSize(6).allMatch(s -> s.n() == 3);
        assertThat(report.getEnvironment()).containsEntry("mongoWriteConcernClient", "test");
        verify(guard).assertTargetsAllowed(true, true);
    }

    @Test
    void prepare_time_is_not_measured() {
        RecordingBenchmark b = new RecordingBenchmark();
        b.prepareSleepMs = 50;
        BenchmarkReport report = b.runSync(config(List.of("rel"), List.of(10), 2, 0));

        assertThat(report.getMeasurements()).hasSize(2)
                .allMatch(m -> m.durationMs() < 40, "dauer ohne prepare");
    }

    @Test
    void database_order_rotates_per_repetition() {
        RecordingBenchmark b = new RecordingBenchmark();
        b.runSync(config(List.of("rel", "mongo", "mongo-ref"), List.of(10), 2, 1));

        List<String> executeOrder = b.calls.stream()
                .filter(c -> c.startsWith("execute:"))
                .map(c -> c.split(":")[1])
                .toList();
        assertThat(executeOrder).containsExactly(
                "rel", "mongo", "mongo-ref",
                "mongo", "mongo-ref", "rel",
                "mongo-ref", "rel", "mongo");
    }

    @Test
    void verify_failure_marks_report_failed_and_keeps_earlier_measurements() {
        RecordingBenchmark b = new RecordingBenchmark();
        b.failOnVerifyDb = "mongo";
        BenchmarkReport report = b.runSync(config(List.of("rel", "mongo"), List.of(10), 2, 0));

        assertThat(report.getStatus()).isEqualTo(BenchmarkReport.Status.FAILED);
        assertThat(report.getError()).contains("zu wenige");
        assertThat(report.getMeasurements()).extracting(BenchmarkMeasurement::database).containsExactly("rel");
    }

    @Test
    void disallowed_host_stops_before_any_prepare() {
        doThrow(new IllegalStateException("host nicht erlaubt")).when(guard).assertTargetsAllowed(anyBoolean(), anyBoolean());
        RecordingBenchmark b = new RecordingBenchmark();
        BenchmarkReport report = b.runSync(config(List.of("rel"), List.of(10), 1, 0));

        assertThat(report.getStatus()).isEqualTo(BenchmarkReport.Status.FAILED);
        assertThat(b.calls).isEmpty();
    }

    @Test
    void only_selected_database_is_touched() {
        RecordingBenchmark b = new RecordingBenchmark();
        b.runSync(config(List.of("mongo-ref"), List.of(10), 1, 0));

        assertThat(b.calls).allMatch(c -> c.contains(":mongo-ref:"));
        verify(guard).assertTargetsAllowed(false, true);
    }

    @Test
    void assert_counts_reports_mismatch() {
        InsertBenchmark.assertCounts("rel", Map.of("customer", 10L), Map.of("customer", 10L));
        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                        InsertBenchmark.assertCounts("rel", Map.of("customer", 10L), Map.of("customer", 9L)))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("customer erwartet 10");
    }

    @Test
    void rotate_shifts_order() {
        assertThat(InsertBenchmark.rotate(List.of("a", "b", "c"), 4)).containsExactly("b", "c", "a");
    }
}
