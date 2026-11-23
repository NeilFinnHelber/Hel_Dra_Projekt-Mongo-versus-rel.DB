package at.spengergasse.sj2526pos1javaseed.Ids;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

public class OrderIdTest {
    @Test void can_create_order_id() { assertThat(new OrderId(1L).getId()).isEqualTo(1L); }
    @Test void equal_ids_are_equal() { assertThat(new OrderId(1L)).isEqualTo(new OrderId(1L)); }
    @Test void different_ids_not_equal() { assertThat(new OrderId(1L)).isNotEqualTo(new OrderId(2L)); }
    @Test void equal_ids_same_hashcode() { assertThat(new OrderId(1L).hashCode()).isEqualTo(new OrderId(1L).hashCode()); }
    @Test void can_set_id() { OrderId id = new OrderId(1L); id.setId(2L); assertThat(id.getId()).isEqualTo(2L); }
}
