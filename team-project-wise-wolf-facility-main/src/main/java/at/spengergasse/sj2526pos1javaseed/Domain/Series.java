package at.spengergasse.sj2526pos1javaseed.Domain;

import at.spengergasse.sj2526pos1javaseed.Ids.SeriesId;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.Set;

@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
public class Series {
    @EmbeddedId
    private SeriesId seriesid;

    String title;
    Integer release_year;
    Integer episodes;

    @ElementCollection(targetClass = Language.class, fetch = FetchType.EAGER)
    @CollectionTable(name = "series_languages", joinColumns = @JoinColumn(name = "series_id"))
    @Enumerated(EnumType.STRING)
    private Set<Language> languages;

    Float length;

    @ElementCollection(targetClass = Genre.class, fetch = FetchType.EAGER)
    @CollectionTable(name = "series_genres", joinColumns = @JoinColumn(name = "series_id"))
    @Enumerated(EnumType.STRING)
    private Set<Genre> genres;
    /*
    record SeriesId(@Generated @NotNull Long id) {

    }

     */
}