package at.spengergasse.sj2526pos1javaseed.serviceTests;

import at.spengergasse.sj2526pos1javaseed.Commands.CreateBillingInformationCommand;
import at.spengergasse.sj2526pos1javaseed.Domain.*;
import at.spengergasse.sj2526pos1javaseed.Ids.BillingInformationId;
import at.spengergasse.sj2526pos1javaseed.Ids.UserId;
import at.spengergasse.sj2526pos1javaseed.persistence.BillingInformationRepository;
import at.spengergasse.sj2526pos1javaseed.persistence.CustomerRepository;
import at.spengergasse.sj2526pos1javaseed.richtypes.Email;
import at.spengergasse.sj2526pos1javaseed.services.BillingInformationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class BillingInformationServiceTest {

    @Mock private BillingInformationRepository billingInformationRepository;
    @Mock private CustomerRepository customerRepository;
    @InjectMocks private BillingInformationService billingInformationService;

    private Customer buildCustomer() {
        return Customer.builder().id(new UserId(1L)).nickname("u1").email(new Email("u1@x.com"))
                .permission(Permission.Customer).firstName("J").lastName("D").build();
    }

    private BillingInformation buildBilling(Long id) {
        return BillingInformation.builder().id(new BillingInformationId(id))
                .cardNumber("4111111111111111").cvv(123)
                .expirationDate(LocalDate.of(2028, 12, 31)).build();
    }

    @Test
    void create_saves_billing_information() {
        CreateBillingInformationCommand cmd = new CreateBillingInformationCommand(
                LocalDate.of(2028, 12, 31), "4111111111111111", 123, 1L);
        when(customerRepository.findById(new UserId(1L))).thenReturn(Optional.of(buildCustomer()));
        when(billingInformationRepository.save(any())).thenReturn(buildBilling(1L));
        BillingInformation result = billingInformationService.create(cmd);
        assertThat(result).isNotNull();
        verify(billingInformationRepository).save(any());
    }

    @Test
    void create_throws_when_customer_not_found() {
        CreateBillingInformationCommand cmd = new CreateBillingInformationCommand(
                LocalDate.of(2028, 12, 31), "4111111111111111", 123, 99L);
        when(customerRepository.findById(any())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> billingInformationService.create(cmd)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void create_throws_when_card_number_blank() {
        CreateBillingInformationCommand cmd = new CreateBillingInformationCommand(
                LocalDate.of(2028, 12, 31), "  ", 123, 1L);
        when(customerRepository.findById(any())).thenReturn(Optional.of(buildCustomer()));
        assertThatThrownBy(() -> billingInformationService.create(cmd)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void create_throws_when_expiration_in_past() {
        CreateBillingInformationCommand cmd = new CreateBillingInformationCommand(
                LocalDate.of(2000, 1, 1), "4111111111111111", 123, 1L);
        when(customerRepository.findById(any())).thenReturn(Optional.of(buildCustomer()));
        assertThatThrownBy(() -> billingInformationService.create(cmd)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void getById_returns_billing_information() {
        when(billingInformationRepository.findById(new BillingInformationId(1L))).thenReturn(Optional.of(buildBilling(1L)));
        BillingInformation result = billingInformationService.getById(new BillingInformationId(1L));
        assertThat(result.getCardNumber()).isEqualTo("4111111111111111");
    }

    @Test
    void getById_throws_when_not_found() {
        when(billingInformationRepository.findById(any())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> billingInformationService.getById(new BillingInformationId(99L))).isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void getAll_returns_list() {
        when(billingInformationRepository.findAll()).thenReturn(List.of(buildBilling(1L), buildBilling(2L)));
        assertThat(billingInformationService.getAll()).hasSize(2);
    }

    @Test
    void getAll_paged_returns_page() {
        when(billingInformationRepository.findAll(any(PageRequest.class))).thenReturn(new PageImpl<>(List.of(buildBilling(1L))));
        Page<BillingInformation> result = billingInformationService.getAll(PageRequest.of(0, 10));
        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void delete_calls_deleteById() {
        billingInformationService.deleteBillingInformation(new BillingInformationId(1L));
        verify(billingInformationRepository).deleteById(new BillingInformationId(1L));
    }
}
