package at.spengergasse.sj2526pos1javaseed.Ids;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

public class BillingInformationIdTest {
    @Test void can_create_id() { assertThat(new BillingInformationId(1L).getId()).isEqualTo(1L); }
    @Test void equal_ids_are_equal() { assertThat(new BillingInformationId(1L)).isEqualTo(new BillingInformationId(1L)); }
    @Test void different_ids_not_equal() { assertThat(new BillingInformationId(1L)).isNotEqualTo(new BillingInformationId(2L)); }
    @Test void equal_ids_same_hashcode() { assertThat(new BillingInformationId(1L).hashCode()).isEqualTo(new BillingInformationId(1L).hashCode()); }
}
