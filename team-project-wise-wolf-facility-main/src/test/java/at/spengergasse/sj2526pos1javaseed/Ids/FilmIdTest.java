package at.spengergasse.sj2526pos1javaseed.Ids;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

public class FilmIdTest {
    @Test void can_create_film_id() { assertThat(new FilmId(1L).getId()).isEqualTo(1L); }
    @Test void equal_ids_are_equal() { assertThat(new FilmId(1L)).isEqualTo(new FilmId(1L)); }
    @Test void different_ids_not_equal() { assertThat(new FilmId(1L)).isNotEqualTo(new FilmId(2L)); }
    @Test void equal_ids_same_hashcode() { assertThat(new FilmId(1L).hashCode()).isEqualTo(new FilmId(1L).hashCode()); }
    @Test void can_set_id() { FilmId id = new FilmId(1L); id.setId(2L); assertThat(id.getId()).isEqualTo(2L); }
}
