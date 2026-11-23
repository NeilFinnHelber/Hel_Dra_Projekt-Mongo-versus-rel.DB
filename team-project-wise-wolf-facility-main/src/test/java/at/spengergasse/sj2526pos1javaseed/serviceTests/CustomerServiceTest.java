package at.spengergasse.sj2526pos1javaseed.serviceTests;

import at.spengergasse.sj2526pos1javaseed.Commands.CreateCustomerCommand;
import at.spengergasse.sj2526pos1javaseed.Domain.Customer;
import at.spengergasse.sj2526pos1javaseed.Domain.Permission;
import at.spengergasse.sj2526pos1javaseed.Ids.UserId;
import at.spengergasse.sj2526pos1javaseed.persistence.CustomerRepository;
import at.spengergasse.sj2526pos1javaseed.richtypes.Email;
import at.spengergasse.sj2526pos1javaseed.services.CustomerService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private CustomerService customerService;

    private Customer buildCustomer(Long id) {
        return Customer.builder()
                .id(new UserId(id))
                .nickname("user" + id)
                .email(new Email("user" + id + "@example.com"))
                .permission(Permission.Customer)
                .firstName("John")
                .lastName("Doe")
                .build();
    }

    @Test
    void create_saves_and_returns_customer() {
        CreateCustomerCommand command = new CreateCustomerCommand("John", "Doe", new Email("john@example.com"));
        Customer customer = buildCustomer(1L);
        when(customerRepository.save(any())).thenReturn(customer);

        Customer result = customerService.create(command);

        assertThat(result).isNotNull();
        verify(customerRepository).save(any());
    }

    @Test
    void create_throws_if_firstName_blank() {
        CreateCustomerCommand command = new CreateCustomerCommand("  ", "Doe", new Email("john@example.com"));
        assertThatThrownBy(() -> customerService.create(command))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void getById_returns_customer_when_found() {
        Customer customer = buildCustomer(1L);
        when(customerRepository.findById(new UserId(1L))).thenReturn(Optional.of(customer));

        Customer result = customerService.getById(new UserId(1L));

        assertThat(result.getFirstName()).isEqualTo("John");
    }

    @Test
    void getById_throws_when_not_found() {
        when(customerRepository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerService.getById(new UserId(99L)))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void getAll_returns_list() {
        when(customerRepository.findAll()).thenReturn(List.of(buildCustomer(1L), buildCustomer(2L)));

        List<Customer> result = customerService.getAll();

        assertThat(result).hasSize(2);
    }

    @Test
    void getAll_paged_returns_page() {
        Page<Customer> page = new PageImpl<>(List.of(buildCustomer(1L)));
        when(customerRepository.findAll(any(PageRequest.class))).thenReturn(page);

        Page<Customer> result = customerService.getAll(PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void updateCustomer_calls_save() {
        Customer customer = buildCustomer(1L);
        customerService.updateCustomer(customer);
        verify(customerRepository).save(customer);
    }

    @Test
    void deleteCustomer_calls_deleteById() {
        customerService.deleteCustomer(new UserId(1L));
        verify(customerRepository).deleteById(new UserId(1L));
    }
}
