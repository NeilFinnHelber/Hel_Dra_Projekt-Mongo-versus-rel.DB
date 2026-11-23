package at.spengergasse.sj2526pos1javaseed.Ids;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

public class SeriesIdTest {
    @Test void can_create_series_id() { assertThat(new SeriesId(1L).getId()).isEqualTo(1L); }
    @Test void equal_ids_are_equal() { assertThat(new SeriesId(1L)).isEqualTo(new SeriesId(1L)); }
    @Test void different_ids_not_equal() { assertThat(new SeriesId(1L)).isNotEqualTo(new SeriesId(2L)); }
    @Test void equal_ids_same_hashcode() { assertThat(new SeriesId(1L).hashCode()).isEqualTo(new SeriesId(1L).hashCode()); }
    @Test void can_set_id() { SeriesId id = new SeriesId(1L); id.setId(2L); assertThat(id.getId()).isEqualTo(2L); }
}
