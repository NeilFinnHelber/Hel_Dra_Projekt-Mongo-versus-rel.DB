package at.spengergasse.sj2526pos1javaseed.dto;

import at.spengergasse.sj2526pos1javaseed.Domain.Genre;
import at.spengergasse.sj2526pos1javaseed.Domain.Language;
import at.spengergasse.sj2526pos1javaseed.Domain.Series;

import java.util.Set;

public record SeriesDTO(
        Long id,
        String title,
        Integer releaseYear,
        Integer episodes,
        Float length,
        Set<Language> languages,
        Set<Genre> genres
) {
    public static SeriesDTO from(Series series) {
        return new SeriesDTO(
                series.getSeriesid() != null ? series.getSeriesid().getId() : null,
                series.getTitle(),
                series.getRelease_year(),
                series.getEpisodes(),
                series.getLength(),
                series.getLanguages(),
                series.getGenres()
        );
    }
}
