package at.spengergasse.sj2526pos1javaseed.presentation;

import at.spengergasse.sj2526pos1javaseed.config.SecurityConfig;
import at.spengergasse.sj2526pos1javaseed.services.FilmService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = FilmController.class)
@Import(SecurityConfig.class)
public class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FilmService filmService;

    // ── FilmController ───────────────────────────────────────────────────────

    @Test
    void getFilms_anonymous_isOk() throws Exception {
        Page<at.spengergasse.sj2526pos1javaseed.Domain.Film> page = new PageImpl<>(List.of());
        when(filmService.getAllFilms(any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/v1.0.0/filme")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    void postFilm_anonymous_isRedirectedToLogin() throws Exception {
        String json = """
                {
                    "title": "Inception",
                    "releaseYear": 2010,
                    "length": 148.0,
                    "languages": ["English"],
                    "genres": ["Action", "SciFi"]
                }
                """;

        mockMvc.perform(post("/api/v1.0.0/filme")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void postFilm_admin_isOk() throws Exception {
        at.spengergasse.sj2526pos1javaseed.Domain.Film film =
                at.spengergasse.sj2526pos1javaseed.Domain.Film.builder()
                        .filmid(new at.spengergasse.sj2526pos1javaseed.Ids.FilmId(1L))
                        .title("Inception")
                        .build();
        when(filmService.create(any())).thenReturn(film);

        String json = """
                {
                    "title": "Inception",
                    "releaseYear": 2010,
                    "length": 148.0,
                    "languages": ["English"],
                    "genres": ["Action", "SciFi"]
                }
                """;

        mockMvc.perform(post("/api/v1.0.0/filme")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "employee", roles = {"EMPLOYEE"})
    void postFilm_employee_isOk() throws Exception {
        at.spengergasse.sj2526pos1javaseed.Domain.Film film =
                at.spengergasse.sj2526pos1javaseed.Domain.Film.builder()
                        .filmid(new at.spengergasse.sj2526pos1javaseed.Ids.FilmId(2L))
                        .title("The Matrix")
                        .build();
        when(filmService.create(any())).thenReturn(film);

        String json = """
                {
                    "title": "The Matrix",
                    "releaseYear": 1999,
                    "length": 136.0,
                    "languages": ["English"],
                    "genres": ["SciFi", "Action"]
                }
                """;

        mockMvc.perform(post("/api/v1.0.0/filme")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "customer", roles = {"CUSTOMER"})
    void postFilm_customer_isForbidden() throws Exception {
        String json = """
                {
                    "title": "Forbidden Film",
                    "releaseYear": 2020,
                    "length": 90.0,
                    "languages": ["English"],
                    "genres": ["Drama"]
                }
                """;

        mockMvc.perform(post("/api/v1.0.0/filme")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "customer", roles = {"CUSTOMER"})
    void getFilm_customer_canRead() throws Exception {
        Page<at.spengergasse.sj2526pos1javaseed.Domain.Film> page = new PageImpl<>(List.of());
        when(filmService.getAllFilms(any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/v1.0.0/filme")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }
}
