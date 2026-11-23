package at.spengergasse.sj2526pos1javaseed.Ids;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

public class CustomerIdTest {
    @Test void can_create_customer_id() { assertThat(new CustomerId(1L).getId()).isEqualTo(1L); }
    @Test void equal_ids_are_equal() { assertThat(new CustomerId(1L)).isEqualTo(new CustomerId(1L)); }
    @Test void different_ids_not_equal() { assertThat(new CustomerId(1L)).isNotEqualTo(new CustomerId(2L)); }
    @Test void equal_ids_same_hashcode() { assertThat(new CustomerId(1L).hashCode()).isEqualTo(new CustomerId(1L).hashCode()); }
}
