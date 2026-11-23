package at.spengergasse.sj2526pos1javaseed.persistence;

import at.spengergasse.sj2526pos1javaseed.Domain.*;
import at.spengergasse.sj2526pos1javaseed.Ids.FilmId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class FilmRepositoryTest {

    @Mock
    private FilmRepository filmRepository;

    private Film buildFilm(Long id, String title) {
        return Film.builder()
                .filmid(new FilmId(id))
                .title(title)
                .release_year(2010)
                .length(120.0f)
                .languages(Set.of(Language.English))
                .genres(Set.of(Genre.Action))
                .build();
    }

    @Test
    void can_save_and_find_film_by_id() {
        Film film = buildFilm(1L, "Inception");
        when(filmRepository.save(film)).thenReturn(film);
        when(filmRepository.findById(new FilmId(1L))).thenReturn(Optional.of(film));

        Film saved = filmRepository.save(film);
        Optional<Film> found = filmRepository.findById(new FilmId(1L));

        assertThat(saved.getTitle()).isEqualTo("Inception");
        assertThat(found).isPresent();
        assertThat(found.get().getTitle()).isEqualTo("Inception");
        verify(filmRepository).save(film);
        verify(filmRepository).findById(new FilmId(1L));
    }

    @Test
    void can_find_film_by_title() {
        Film film = buildFilm(2L, "Interstellar");
        when(filmRepository.findFilmByTitle("Interstellar")).thenReturn(film);

        Film found = filmRepository.findFilmByTitle("Interstellar");

        assertThat(found).isNotNull();
        assertThat(found.getTitle()).isEqualTo("Interstellar");
        verify(filmRepository).findFilmByTitle("Interstellar");
    }

    @Test
    void can_find_all_films() {
        List<Film> films = List.of(buildFilm(1L, "Film A"), buildFilm(2L, "Film B"));
        when(filmRepository.findAll()).thenReturn(films);

        List<Film> result = filmRepository.findAll();

        assertThat(result).hasSize(2);
        verify(filmRepository).findAll();
    }

    @Test
    void returns_empty_when_film_not_found() {
        when(filmRepository.findById(new FilmId(99L))).thenReturn(Optional.empty());

        Optional<Film> result = filmRepository.findById(new FilmId(99L));

        assertThat(result).isEmpty();
    }

    @Test
    void can_delete_film() {
        filmRepository.deleteById(new FilmId(1L));
        verify(filmRepository, times(1)).deleteById(new FilmId(1L));
    }
}
