package at.spengergasse.sj2526pos1javaseed.Ids;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

public class Order_PlacementIdTest {
    @Test void can_create_id() { assertThat(new Order_PlacementId(1L).getId()).isEqualTo(1L); }
    @Test void equal_ids_are_equal() { assertThat(new Order_PlacementId(1L)).isEqualTo(new Order_PlacementId(1L)); }
    @Test void different_ids_not_equal() { assertThat(new Order_PlacementId(1L)).isNotEqualTo(new Order_PlacementId(2L)); }
    @Test void equal_ids_same_hashcode() { assertThat(new Order_PlacementId(1L).hashCode()).isEqualTo(new Order_PlacementId(1L).hashCode()); }
    @Test void can_set_id() { Order_PlacementId id = new Order_PlacementId(1L); id.setId(2L); assertThat(id.getId()).isEqualTo(2L); }
}
