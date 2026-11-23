package at.spengergasse.sj2526pos1javaseed.serviceTests;

import at.spengergasse.sj2526pos1javaseed.Commands.CreateFilmCommand;
import at.spengergasse.sj2526pos1javaseed.Domain.Film;
import at.spengergasse.sj2526pos1javaseed.Domain.Genre;
import at.spengergasse.sj2526pos1javaseed.Domain.Language;
import at.spengergasse.sj2526pos1javaseed.Ids.FilmId;
import at.spengergasse.sj2526pos1javaseed.persistence.FilmRepository;
import at.spengergasse.sj2526pos1javaseed.persistence.StoreRepository;
import at.spengergasse.sj2526pos1javaseed.services.FilmService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class FilmServiceTest {

    @Mock
    private FilmRepository filmRepository;

    @Mock
    private StoreRepository storeRepository;

    @InjectMocks
    private FilmService filmService;

    private Film buildFilm(Long id) {
        return Film.builder()
                .filmid(new FilmId(id))
                .title("Film " + id)
                .release_year(2020)
                .length(120.0f)
                .languages(Set.of(Language.English))
                .genres(Set.of(Genre.Action))
                .build();
    }

    @Test
    void create_saves_and_returns_film() {
        CreateFilmCommand command = new CreateFilmCommand("Inception", 2010, 148.0f, Set.of(Language.English), Set.of(Genre.SciFi));
        Film film = buildFilm(1L);
        when(filmRepository.save(any())).thenReturn(film);

        Film result = filmService.create(command);

        assertThat(result).isNotNull();
        verify(filmRepository).save(any());
    }

    @Test
    void getFilmById_returns_film_when_found() {
        Film film = buildFilm(1L);
        when(filmRepository.findById(new FilmId(1L))).thenReturn(Optional.of(film));

        Film result = filmService.getFilmById(new FilmId(1L));

        assertThat(result.getTitle()).isEqualTo("Film 1");
    }

    @Test
    void getFilmById_throws_when_not_found() {
        when(filmRepository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> filmService.getFilmById(new FilmId(99L)))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void getAllFilms_returns_list() {
        when(filmRepository.findAll()).thenReturn(List.of(buildFilm(1L), buildFilm(2L)));

        List<Film> result = filmService.getAllFilms();

        assertThat(result).hasSize(2);
    }

    @Test
    void getAllFilms_paged_returns_page() {
        Page<Film> page = new PageImpl<>(List.of(buildFilm(1L)));
        when(filmRepository.findAll(any(PageRequest.class))).thenReturn(page);

        Page<Film> result = filmService.getAllFilms(PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void updateProduct_calls_save() {
        Film film = buildFilm(1L);
        when(filmRepository.save(film)).thenReturn(film);

        Film result = filmService.updateProduct(film);

        assertThat(result).isEqualTo(film);
        verify(filmRepository).save(film);
    }

    @Test
    void deleteFilm_removes_from_stores_and_deletes() {
        when(storeRepository.findAll()).thenReturn(List.of());

        filmService.deleteFilm(1L);

        verify(filmRepository).deleteById(new FilmId(1L));
    }
}
