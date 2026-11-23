package at.spengergasse.sj2526pos1javaseed.persistence;

import at.spengergasse.sj2526pos1javaseed.Domain.BillingInformation;
import at.spengergasse.sj2526pos1javaseed.Ids.BillingInformationId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class BillingInformationRepositoryTest {

    @Mock
    private BillingInformationRepository billingInformationRepository;

    private BillingInformation buildBilling(Long id, String cardNumber) {
        return BillingInformation.builder()
                .id(new BillingInformationId(id))
                .cardNumber(cardNumber)
                .cvv(123)
                .expirationDate(LocalDate.of(2028, 12, 31))
                .build();
    }

    @Test
    void can_save_and_find_billing_information_by_id() {
        BillingInformation billing = buildBilling(1L, "4111111111111111");
        when(billingInformationRepository.save(billing)).thenReturn(billing);
        when(billingInformationRepository.findById(new BillingInformationId(1L))).thenReturn(Optional.of(billing));

        BillingInformation saved = billingInformationRepository.save(billing);
        Optional<BillingInformation> found = billingInformationRepository.findById(new BillingInformationId(1L));

        assertThat(saved.getCardNumber()).isEqualTo("4111111111111111");
        assertThat(found).isPresent();
        assertThat(found.get().getCvv()).isEqualTo(123);
        verify(billingInformationRepository).save(billing);
        verify(billingInformationRepository).findById(new BillingInformationId(1L));
    }

    @Test
    void can_find_all_billing_information() {
        List<BillingInformation> billings = List.of(
                buildBilling(1L, "1234567890123456"),
                buildBilling(2L, "6543210987654321")
        );
        when(billingInformationRepository.findAll()).thenReturn(billings);

        List<BillingInformation> result = billingInformationRepository.findAll();

        assertThat(result).hasSize(2);
        verify(billingInformationRepository).findAll();
    }

    @Test
    void returns_empty_when_billing_information_not_found() {
        when(billingInformationRepository.findById(new BillingInformationId(99L))).thenReturn(Optional.empty());

        Optional<BillingInformation> result = billingInformationRepository.findById(new BillingInformationId(99L));

        assertThat(result).isEmpty();
    }

    @Test
    void can_delete_billing_information() {
        billingInformationRepository.deleteById(new BillingInformationId(1L));
        verify(billingInformationRepository, times(1)).deleteById(new BillingInformationId(1L));
    }
}
