package at.spengergasse.sj2526pos1javaseed.services;

import at.spengergasse.sj2526pos1javaseed.Commands.CreateFilmCommand;
import at.spengergasse.sj2526pos1javaseed.Domain.Film;
import at.spengergasse.sj2526pos1javaseed.Ids.FilmId;
import at.spengergasse.sj2526pos1javaseed.persistence.FilmRepository;
import at.spengergasse.sj2526pos1javaseed.persistence.StoreRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RequiredArgsConstructor

@Service
@Transactional(readOnly = true)
public class FilmService {
    private final FilmRepository filmRepository;
    private final StoreRepository storeRepository;

    // neuen film aus dem command erstellen und in der datenbank speichern
    @Transactional
    public Film create(@NotNull @Valid CreateFilmCommand command) {

        Film film = Film.builder()
                .filmid(new FilmId(System.currentTimeMillis()))
                .title(command.title())
                .release_year(command.releaseYear())
                .length(command.length())
                .languages(command.languages())
                .genres(command.genres())
                .build();

        Film saved = filmRepository.save(film);
        //filmRepository.findFilmByTitle(command.title());
        return saved;
    }

    // einen film anhand der id suchen, fehler wenn nicht gefunden
    public Film getFilmById(FilmId filmId) {
        return filmRepository.findById(filmId)
                .orElseThrow(() -> new java.util.NoSuchElementException("Film not found"));
    }

    // film direkt speichern (z.b. nach externen änderungen)
    public void saveProduct(Film film) {
        filmRepository.save(film);
    }

    // vorhandenen film aktualisieren und zurückgeben
    @Transactional
    public Film updateProduct(Film film) {
        return filmRepository.save(film);
    }

    // film löschen: zuerst aus allen stores entfernen, dann aus der datenbank
    @Transactional
    public void deleteFilm(Long id) {
        FilmId filmId = new FilmId(id);
        var storesWithFilm = storeRepository.findAll().stream()
                .filter(store -> store.getFilms().stream().anyMatch(f -> f.getFilmid().equals(filmId)))
                .toList();
        for (var store : storesWithFilm) {
            store.getFilms().removeIf(f -> f.getFilmid().equals(filmId));
            storeRepository.save(store);
        }
        filmRepository.deleteById(filmId);
    }

    // alle filme als liste zurückgeben
    public List<Film> getAllFilms() {
        return filmRepository.findAll();
    }

    // alle filme mit seitenweise (pagination) zurückgeben
    public Page<Film> getAllFilms(Pageable pageable) {
        return filmRepository.findAll(pageable);
    }

    //push


}

