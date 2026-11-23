package at.spengergasse.sj2526pos1javaseed.Ids;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

public class UserIdTest {
    @Test void can_create_user_id() { assertThat(new UserId(1L).getId()).isEqualTo(1L); }
    @Test void equal_ids_are_equal() { assertThat(new UserId(1L)).isEqualTo(new UserId(1L)); }
    @Test void different_ids_not_equal() { assertThat(new UserId(1L)).isNotEqualTo(new UserId(2L)); }
    @Test void equal_ids_same_hashcode() { assertThat(new UserId(1L).hashCode()).isEqualTo(new UserId(1L).hashCode()); }
    @Test void can_set_id() { UserId id = new UserId(1L); id.setId(2L); assertThat(id.getId()).isEqualTo(2L); }
}
