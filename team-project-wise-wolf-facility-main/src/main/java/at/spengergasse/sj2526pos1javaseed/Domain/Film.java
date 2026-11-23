package at.spengergasse.sj2526pos1javaseed.Domain;

import at.spengergasse.sj2526pos1javaseed.Ids.FilmId;
import jakarta.persistence.*;
import lombok.*;

import java.util.Set;


@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
public class Film {

    @EmbeddedId
    private FilmId filmid;

    String title;
    Integer release_year;
    Float length;

    @ElementCollection(targetClass = Language.class, fetch = FetchType.EAGER)
    @CollectionTable(
            name = "film_languages",
            joinColumns = @JoinColumn(name = "film_id")
    )
    @Enumerated(EnumType.STRING)
    private Set<Language> languages;

    @ElementCollection(targetClass = Genre.class, fetch = FetchType.EAGER)
    @CollectionTable(
            name = "film_genres",
            joinColumns = @JoinColumn(name = "film_id")
    )
    @Enumerated(EnumType.STRING)
    private Set<Genre> genres;

    @Column(name = "poster")
    private byte[] poster;

}