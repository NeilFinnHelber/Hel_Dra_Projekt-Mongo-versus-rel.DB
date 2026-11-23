package at.spengergasse.sj2526pos1javaseed.Ids;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

public class Phone_NumberIdTest {
    @Test void can_create_id() { assertThat(new Phone_NumberId(1L).getId()).isEqualTo(1L); }
    @Test void equal_ids_are_equal() { assertThat(new Phone_NumberId(1L)).isEqualTo(new Phone_NumberId(1L)); }
    @Test void different_ids_not_equal() { assertThat(new Phone_NumberId(1L)).isNotEqualTo(new Phone_NumberId(2L)); }
    @Test void equal_ids_same_hashcode() { assertThat(new Phone_NumberId(1L).hashCode()).isEqualTo(new Phone_NumberId(1L).hashCode()); }
}
