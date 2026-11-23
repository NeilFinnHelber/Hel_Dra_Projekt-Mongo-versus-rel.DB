package at.spengergasse.sj2526pos1javaseed.presentation;

import at.spengergasse.sj2526pos1javaseed.Domain.Series;
import at.spengergasse.sj2526pos1javaseed.Ids.SeriesId;
import at.spengergasse.sj2526pos1javaseed.services.SeriesService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;
import java.util.NoSuchElementException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = SeriesController.class, excludeAutoConfiguration = {org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class, org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration.class})
public class SeriesControllerTest {

    @MockitoBean
    private SeriesService seriesService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setup(WebApplicationContext webApplicationContext) {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .build();
    }

    @Test
    void getAllSeries_returns_paged_DTO() throws Exception {
        Series series = Series.builder()
                .seriesid(new SeriesId(1L))
                .title("Breaking Bad")
                .build();

        Page<Series> page = new PageImpl<>(List.of(series));
        when(seriesService.getAll(any(Pageable.class))).thenReturn(page);

        this.mockMvc.perform(get("/api/v1.0.0/series")
                        .param("page", "0")
                        .param("size", "10")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.seriesDTOList").isArray());

        verify(seriesService, times(1)).getAll(any(Pageable.class));
    }

    @Test
    void getSeries_returns_series() throws Exception {
        Series series = Series.builder()
                .seriesid(new SeriesId(1L))
                .title("Breaking Bad")
                .build();

        when(seriesService.getById(new SeriesId(1L))).thenReturn(series);

        this.mockMvc.perform(get("/api/v1.0.0/series/1")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Breaking Bad"));

        verify(seriesService, times(1)).getById(new SeriesId(1L));
    }

    @Test
    void getSeries_returns_not_found() throws Exception {
        when(seriesService.getById(new SeriesId(1L))).thenThrow(new NoSuchElementException());

        this.mockMvc.perform(get("/api/v1.0.0/series/1")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    void createSeries_returns_created_series() throws Exception {
        Series series = Series.builder()
                .seriesid(new SeriesId(1L))
                .title("Breaking Bad")
                .build();

        when(seriesService.create(any())).thenReturn(series);

        String json = """
                {
                    "title": "Breaking Bad",
                    "releaseYear": 2008,
                    "episodes": 62,
                    "length": 45.0,
                    "languages": ["English"],
                    "genres": ["Drama", "Crime"]
                }
                """;

        this.mockMvc.perform(post("/api/v1.0.0/series")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Breaking Bad"));

        verify(seriesService, times(1)).create(any());
    }

    @Test
    void updateSeries_returns_updated_series() throws Exception {
        Series series = Series.builder()
                .seriesid(new SeriesId(1L))
                .title("Breaking Bad")
                .build();

        when(seriesService.getById(new SeriesId(1L))).thenReturn(series);

        String json = """
                {
                    "title": "Breaking Bad",
                    "releaseYear": 2008,
                    "episodes": 62,
                    "length": 45.0,
                    "languages": ["English"],
                    "genres": ["Drama", "Crime"]
                }
                """;

        this.mockMvc.perform(patch("/api/v1.0.0/series/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Breaking Bad"));

        verify(seriesService, times(1)).updateSeries(any(Series.class));
    }

    @Test
    void deleteSeries_returns_no_content() throws Exception {
        this.mockMvc.perform(delete("/api/v1.0.0/series/1"))
                .andExpect(status().isNoContent());

        verify(seriesService, times(1)).deleteSeries(new SeriesId(1L));
    }
}
