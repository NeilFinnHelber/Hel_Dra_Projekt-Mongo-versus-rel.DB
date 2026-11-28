package at.spengergasse.sj2526pos1javaseed.benchmark.write;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** http vertrag, den k6 (MODE=insert) nutzt */
@WebMvcTest(value = InsertBenchmarkController.class,
        properties = "app.testdata.endpoint-enabled=true",
        excludeAutoConfiguration = {
                org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
                org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration.class})
class InsertBenchmarkControllerTest {

    @MockitoBean
    private InsertBenchmark benchmark;

    @Autowired
    private MockMvc mockMvc;

    private static BenchmarkReport finishedReport() {
        BenchmarkReport report = new BenchmarkReport(3,
                new BenchmarkConfig(List.of("rel"), List.of(100), 1, 0, 42L, 1000));
        report.add(BenchmarkMeasurement.of(1, "insert", "rel", 100, 1, false, 12_500_000, 100,
                Instant.parse("2026-11-28T10:00:00Z")));
        report.finish();
        return report;
    }

    @Test
    void start_without_body_uses_defaults_and_returns_202() throws Exception {
        when(benchmark.start(any())).thenAnswer(inv -> new BenchmarkReport(1, inv.getArgument(0)));

        mockMvc.perform(post("/tests/insert"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("RUNNING"))
                .andExpect(jsonPath("$.config.scales.length()").value(3))
                .andExpect(jsonPath("$.totalSteps").value(3 * 3 * 6));
        verify(benchmark).start(argThat(c -> c.repetitions() == 5 && c.warmups() == 1));
    }

    @Test
    void start_with_partial_body() throws Exception {
        when(benchmark.start(any())).thenAnswer(inv -> new BenchmarkReport(2, inv.getArgument(0)));

        mockMvc.perform(post("/tests/insert").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"databases\":[\"rel\",\"mongo\"],\"scales\":[100],\"repetitions\":3}"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.config.databases.length()").value(2))
                .andExpect(jsonPath("$.config.warmups").value(1));
    }

    @Test
    void invalid_config_is_400_and_nothing_starts() throws Exception {
        mockMvc.perform(post("/tests/insert").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"databases\":[\"oracle\"]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(org.hamcrest.Matchers.containsString("oracle")));
        verify(benchmark, never()).start(any());
    }

    @Test
    void get_report_and_csv() throws Exception {
        when(benchmark.get(3)).thenReturn(Optional.of(finishedReport()));

        mockMvc.perform(get("/tests/insert/3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FINISHED"))
                .andExpect(jsonPath("$.measurements.length()").value(1));
        mockMvc.perform(get("/tests/insert/3/measurements.csv"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/csv"))
                .andExpect(content().string(org.hamcrest.Matchers.startsWith(BenchmarkCsv.MEASUREMENT_HEADER)))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "1,rel,100,false,FINISHED,insert,12.500,1,false,100")));
        mockMvc.perform(get("/tests/insert/3/summary.csv"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.startsWith(BenchmarkCsv.SUMMARY_HEADER)));
    }

    @Test
    void unknown_id_is_404() throws Exception {
        when(benchmark.get(99)).thenReturn(Optional.empty());
        mockMvc.perform(get("/tests/insert/99")).andExpect(status().isNotFound());
        mockMvc.perform(get("/tests/insert/99/measurements.csv")).andExpect(status().isNotFound());
    }
}
