package at.spengergasse.sj2526pos1javaseed.presentation;

import at.spengergasse.sj2526pos1javaseed.Domain.Film;
import at.spengergasse.sj2526pos1javaseed.Domain.Series;
import at.spengergasse.sj2526pos1javaseed.Domain.Customer;
import at.spengergasse.sj2526pos1javaseed.Domain.Order;
import at.spengergasse.sj2526pos1javaseed.Domain.Store;
import at.spengergasse.sj2526pos1javaseed.Ids.*;
import at.spengergasse.sj2526pos1javaseed.services.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = ViewController.class,
        excludeAutoConfiguration = {
                org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
                org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration.class
        })
public class ViewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean private FilmService filmService;
    @MockitoBean private SeriesService seriesService;
    @MockitoBean private CustomerService customerService;
    @MockitoBean private OrderService orderService;
    @MockitoBean private StoreService storeService;

    // ── index ────────────────────────────────────────────────────────────────

    @Test
    void index_returns_200_with_counts() throws Exception {
        when(filmService.getAllFilms()).thenReturn(List.of());
        when(seriesService.getAll()).thenReturn(List.of());
        when(customerService.getAll()).thenReturn(List.of());
        when(orderService.getAll()).thenReturn(List.of());
        when(storeService.getAll()).thenReturn(List.of());

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(model().attributeExists("filmCount", "seriesCount",
                        "customerCount", "orderCount", "storeCount"));
    }

    @Test
    void index_counts_reflect_service_data() throws Exception {
        when(filmService.getAllFilms()).thenReturn(List.of(
                Film.builder().filmid(new FilmId(1L)).title("A").build(),
                Film.builder().filmid(new FilmId(2L)).title("B").build()
        ));
        when(seriesService.getAll()).thenReturn(List.of());
        when(customerService.getAll()).thenReturn(List.of());
        when(orderService.getAll()).thenReturn(List.of());
        when(storeService.getAll()).thenReturn(List.of());

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("filmCount", 2));
    }

    // ── films ────────────────────────────────────────────────────────────────

    @Test
    void films_returns_200_with_films_model() throws Exception {
        when(filmService.getAllFilms()).thenReturn(List.of(
                Film.builder().filmid(new FilmId(1L)).title("Inception").build()
        ));

        mockMvc.perform(get("/films"))
                .andExpect(status().isOk())
                .andExpect(view().name("films"))
                .andExpect(model().attributeExists("films"));
    }

    @Test
    void films_returns_200_with_empty_list() throws Exception {
        when(filmService.getAllFilms()).thenReturn(List.of());

        mockMvc.perform(get("/films"))
                .andExpect(status().isOk())
                .andExpect(view().name("films"));
    }

    // ── series ───────────────────────────────────────────────────────────────

    @Test
    void series_returns_200_with_seriesList_model() throws Exception {
        when(seriesService.getAll()).thenReturn(List.of(
                Series.builder().seriesid(new SeriesId(1L)).title("Breaking Bad").build()
        ));

        mockMvc.perform(get("/series"))
                .andExpect(status().isOk())
                .andExpect(view().name("series"))
                .andExpect(model().attributeExists("seriesList"));
    }

    // ── customers ────────────────────────────────────────────────────────────

    @Test
    void customers_returns_200_with_customers_model() throws Exception {
        when(customerService.getAll()).thenReturn(List.of(
                Customer.builder().id(new UserId(1L)).firstName("Max").lastName("Muster").build()
        ));

        mockMvc.perform(get("/customers"))
                .andExpect(status().isOk())
                .andExpect(view().name("customers"))
                .andExpect(model().attributeExists("customers"));
    }

    // ── orders ───────────────────────────────────────────────────────────────

    @Test
    void orders_returns_200_with_orders_and_stores_model() throws Exception {
        when(orderService.getAll()).thenReturn(List.of());
        when(storeService.getAll()).thenReturn(List.of());

        mockMvc.perform(get("/orders"))
                .andExpect(status().isOk())
                .andExpect(view().name("orders"))
                .andExpect(model().attributeExists("orders", "stores"));
    }

    // ── stores ───────────────────────────────────────────────────────────────

    @Test
    void stores_returns_200_with_stores_films_series_model() throws Exception {
        when(storeService.getAll()).thenReturn(List.of());
        when(filmService.getAllFilms()).thenReturn(List.of());
        when(seriesService.getAll()).thenReturn(List.of());

        mockMvc.perform(get("/stores"))
                .andExpect(status().isOk())
                .andExpect(view().name("stores"))
                .andExpect(model().attributeExists("stores", "films", "seriesList"));
    }

    // ── docs redirect ────────────────────────────────────────────────────────

    @Test
    void docs_redirects_to_docs_index() throws Exception {
        mockMvc.perform(get("/docs"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/docs/index.html"));
    }
}
