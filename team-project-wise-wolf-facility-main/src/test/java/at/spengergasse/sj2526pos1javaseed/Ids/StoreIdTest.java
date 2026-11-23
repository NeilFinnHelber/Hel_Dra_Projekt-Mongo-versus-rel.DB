package at.spengergasse.sj2526pos1javaseed.Ids;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

public class StoreIdTest {
    @Test void can_create_store_id() { assertThat(new StoreId(1L).getId()).isEqualTo(1L); }
    @Test void equal_ids_are_equal() { assertThat(new StoreId(1L)).isEqualTo(new StoreId(1L)); }
    @Test void different_ids_not_equal() { assertThat(new StoreId(1L)).isNotEqualTo(new StoreId(2L)); }
    @Test void equal_ids_same_hashcode() { assertThat(new StoreId(1L).hashCode()).isEqualTo(new StoreId(1L).hashCode()); }
    @Test void can_set_id() { StoreId id = new StoreId(1L); id.setId(2L); assertThat(id.getId()).isEqualTo(2L); }
}
