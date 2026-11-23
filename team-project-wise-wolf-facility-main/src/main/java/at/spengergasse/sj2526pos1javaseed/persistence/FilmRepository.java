package at.spengergasse.sj2526pos1javaseed.persistence;

import at.spengergasse.sj2526pos1javaseed.Domain.Film;
import at.spengergasse.sj2526pos1javaseed.Ids.FilmId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FilmRepository extends JpaRepository<Film, FilmId> {
    Film findFilmByTitle(String title);
}
