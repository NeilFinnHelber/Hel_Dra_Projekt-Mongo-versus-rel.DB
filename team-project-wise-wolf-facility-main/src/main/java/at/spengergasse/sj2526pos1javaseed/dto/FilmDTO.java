package at.spengergasse.sj2526pos1javaseed.dto;

import at.spengergasse.sj2526pos1javaseed.Domain.Film;
import at.spengergasse.sj2526pos1javaseed.Domain.Genre;
import at.spengergasse.sj2526pos1javaseed.Domain.Language;

import java.util.Set;

public record FilmDTO(
        Long id,
        String title,
        Integer releaseYear,
        Float length,
        Set<Language> languages,
        Set<Genre> genres,
        Boolean hasPoster
) {
    public static FilmDTO from(Film film) {
        return new FilmDTO(
                film.getFilmid() != null ? film.getFilmid().getId() : null,
                film.getTitle(),
                film.getRelease_year(),
                film.getLength(),
                film.getLanguages(),
                film.getGenres(),
                film.getPoster() != null
        );
    }
}
