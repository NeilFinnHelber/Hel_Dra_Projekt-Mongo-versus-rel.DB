package at.spengergasse.sj2526pos1javaseed.presentation;

import at.spengergasse.sj2526pos1javaseed.Domain.Film;
import at.spengergasse.sj2526pos1javaseed.Ids.FilmId;
import at.spengergasse.sj2526pos1javaseed.services.FilmService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;
import java.util.NoSuchElementException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;

@WebMvcTest(value = FilmController.class, excludeAutoConfiguration = {org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class, org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration.class})
public class FilmControllerTest {

    @MockitoBean
    private FilmService filmService;

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
    void getAllFilms_returns_paged_films() throws Exception {
        Film film = Film.builder()
                .filmid(new FilmId(1L))
                .title("Inception")
                .build();

        Page<Film> page = new PageImpl<>(List.of(film));
        when(filmService.getAllFilms(any(Pageable.class))).thenReturn(page);

        this.mockMvc.perform(get("/api/v1.0.0/filme")
                        .param("page", "0")
                        .param("size", "10")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.filmDTOList").isArray());

        verify(filmService, times(1)).getAllFilms(any(Pageable.class));
    }

    @Test
    void getFilm_returns_film() throws Exception {
        Film film = Film.builder()
                .filmid(new FilmId(1L))
                .title("Inception")
                .build();

        when(filmService.getFilmById(new FilmId(1L))).thenReturn(film);

        this.mockMvc.perform(get("/api/v1.0.0/filme/1")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Inception"));

        verify(filmService, times(1)).getFilmById(new FilmId(1L));
    }

    @Test
    void getFilm_returns_not_found() throws Exception {
        when(filmService.getFilmById(new FilmId(1L))).thenThrow(new NoSuchElementException());

        this.mockMvc.perform(get("/api/v1.0.0/filme/1")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    void createFilm_returns_created_film() throws Exception {
        Film film = Film.builder()
                .filmid(new FilmId(1L))
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

        this.mockMvc.perform(post("/api/v1.0.0/filme")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Inception"));

        verify(filmService, times(1)).create(any());
    }

    @Test
    void updateFilm_returns_updated_film() throws Exception {
        Film film = Film.builder()
                .filmid(new FilmId(1L))
                .title("Inception")
                .build();

        when(filmService.getFilmById(new FilmId(1L))).thenReturn(film);
        when(filmService.updateProduct(any(Film.class))).thenReturn(film);

        String json = """
                {
                    "title": "Inception",
                    "releaseYear": 2010,
                    "length": 148.0,
                    "languages": ["English"],
                    "genres": ["Action", "SciFi"]
                }
                """;

        this.mockMvc.perform(patch("/api/v1.0.0/filme/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Inception"));

        verify(filmService, times(1)).updateProduct(any(Film.class));
    }

    @Test
    void deleteFilm_returns_ok() throws Exception {
        this.mockMvc.perform(delete("/api/v1.0.0/filme/1"))
                .andExpect(status().isOk());

        verify(filmService, times(1)).deleteFilm(1L);
    }

    @Test
    void uploadFilmPoster_returns_ok() throws Exception {
        Film film = Film.builder()
                .filmid(new FilmId(1L))
                .title("Inception")
                .build();

        when(filmService.getFilmById(new FilmId(1L))).thenReturn(film);
        when(filmService.updateProduct(any(Film.class))).thenReturn(film);

        MockMultipartFile mockFile = new MockMultipartFile(
                "file", "poster.jpg", MediaType.IMAGE_JPEG_VALUE, "fake-image-bytes".getBytes()
        );

        this.mockMvc.perform(multipart("/api/v1.0.0/filme/1/poster")
                        .file(mockFile))
                .andExpect(status().isOk());

        verify(filmService, times(1)).updateProduct(any(Film.class));
    }

    @Test
    void uploadFilmPoster_returns_not_found_for_unknown_film() throws Exception {
        when(filmService.getFilmById(new FilmId(99L))).thenThrow(new NoSuchElementException());

        MockMultipartFile mockFile = new MockMultipartFile(
                "file", "poster.jpg", MediaType.IMAGE_JPEG_VALUE, "fake-image-bytes".getBytes()
        );

        this.mockMvc.perform(multipart("/api/v1.0.0/filme/99/poster")
                        .file(mockFile))
                .andExpect(status().is5xxServerError());
    }

    @Test
    void getFilmPoster_returns_image_bytes() throws Exception {
        Film film = Film.builder()
                .filmid(new FilmId(1L))
                .title("Inception")
                .poster("fake-poster-bytes".getBytes())
                .build();

        when(filmService.getFilmById(new FilmId(1L))).thenReturn(film);

        this.mockMvc.perform(get("/api/v1.0.0/filme/1/poster")
                        .accept(MediaType.IMAGE_JPEG_VALUE))
                .andExpect(status().isOk())
                .andExpect(content().bytes("fake-poster-bytes".getBytes()));
    }

    @Test
    void getFilmPoster_returns_not_found_when_no_poster() throws Exception {
        Film film = Film.builder()
                .filmid(new FilmId(1L))
                .title("Inception")
                .build(); // poster is null

        when(filmService.getFilmById(new FilmId(1L))).thenReturn(film);

        this.mockMvc.perform(get("/api/v1.0.0/filme/1/poster")
                        .accept(MediaType.IMAGE_JPEG_VALUE))
                .andExpect(status().isNotFound());
    }
}
