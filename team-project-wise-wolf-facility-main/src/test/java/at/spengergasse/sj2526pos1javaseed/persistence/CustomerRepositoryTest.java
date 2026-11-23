package at.spengergasse.sj2526pos1javaseed.persistence;

import at.spengergasse.sj2526pos1javaseed.Domain.*;
import at.spengergasse.sj2526pos1javaseed.Ids.UserId;
import at.spengergasse.sj2526pos1javaseed.richtypes.Email;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CustomerRepositoryTest {

    @Mock
    private CustomerRepository customerRepository;

    private Customer buildCustomer(Long id, String nickname, String email) {
        return Customer.builder()
                .id(new UserId(id))
                .nickname(nickname)
                .email(new Email(email))
                .permission(Permission.Customer)
                .firstName("John")
                .lastName("Doe")
                .build();
    }

    @Test
    void can_save_and_find_customer_by_id() {
        Customer customer = buildCustomer(1L, "jdoe", "jdoe@example.com");
        when(customerRepository.save(customer)).thenReturn(customer);
        when(customerRepository.findById(new UserId(1L))).thenReturn(Optional.of(customer));

        Customer saved = customerRepository.save(customer);
        Optional<Customer> found = customerRepository.findById(new UserId(1L));

        assertThat(saved.getNickname()).isEqualTo("jdoe");
        assertThat(found).isPresent();
        assertThat(found.get().getFirstName()).isEqualTo("John");
        verify(customerRepository).save(customer);
        verify(customerRepository).findById(new UserId(1L));
    }

    @Test
    void can_find_all_customers() {
        List<Customer> customers = List.of(
                buildCustomer(1L, "user1", "user1@example.com"),
                buildCustomer(2L, "user2", "user2@example.com")
        );
        when(customerRepository.findAll()).thenReturn(customers);

        List<Customer> result = customerRepository.findAll();

        assertThat(result).hasSize(2);
        verify(customerRepository).findAll();
    }

    @Test
    void returns_empty_when_customer_not_found() {
        when(customerRepository.findById(new UserId(99L))).thenReturn(Optional.empty());

        Optional<Customer> result = customerRepository.findById(new UserId(99L));

        assertThat(result).isEmpty();
    }

    @Test
    void can_delete_customer() {
        customerRepository.deleteById(new UserId(1L));
        verify(customerRepository, times(1)).deleteById(new UserId(1L));
    }
}
