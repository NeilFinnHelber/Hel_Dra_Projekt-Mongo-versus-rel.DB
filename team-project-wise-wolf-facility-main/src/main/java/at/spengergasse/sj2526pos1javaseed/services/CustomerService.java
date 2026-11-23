package at.spengergasse.sj2526pos1javaseed.services;

import at.spengergasse.sj2526pos1javaseed.Commands.CreateCustomerCommand;
import at.spengergasse.sj2526pos1javaseed.Domain.Customer;
import at.spengergasse.sj2526pos1javaseed.Domain.Film;
import at.spengergasse.sj2526pos1javaseed.Ids.CustomerId;
import at.spengergasse.sj2526pos1javaseed.Ids.FilmId;
import at.spengergasse.sj2526pos1javaseed.Ids.UserId;
import at.spengergasse.sj2526pos1javaseed.persistence.CustomerRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class CustomerService {

    private final CustomerRepository customerRepository;

    // neuen kunden anlegen und in der datenbank speichern
    @Transactional
    public Customer create(@NotNull @Valid CreateCustomerCommand command) {

        if (command.firstName().isBlank() || command.lastName().isBlank() || command.email() == null) {
            throw new IllegalArgumentException("Invalid customer data");
        }

        Customer customer = Customer.builder()
                .id(new UserId(System.currentTimeMillis())) // feste Zahl/Long für Tests
                .firstName(command.firstName())
                .lastName(command.lastName())
                .email(command.email())
                .nickname(command.firstName().toLowerCase() + "_" + command.lastName().toLowerCase() + "_" + System.currentTimeMillis())
                .permission(at.spengergasse.sj2526pos1javaseed.Domain.Permission.Customer)
                .build();

        return customerRepository.save(customer);
    }

    // kunden direkt speichern
    public void saveCustomer(Customer customer) {
        customerRepository.save(customer);
    }

    // bestehenden kunden aktualisieren
    public void updateCustomer(Customer customer) {
        customerRepository.save(customer);
    }

    // kunden anhand der id löschen
    public void deleteCustomer(UserId id) {
        customerRepository.deleteById(id);
    }

    // einen kunden anhand der id laden, fehler wenn nicht gefunden
    public Customer getById(UserId id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new java.util.NoSuchElementException("Customer not found: " + id));
    }

    // alle kunden als liste zurückgeben
    public java.util.List<Customer> getAll() {
        return customerRepository.findAll();
    }

    // alle kunden seitenweise zurückgeben
    public Page<Customer> getAll(Pageable pageable) {
        return customerRepository.findAll(pageable);
    }
}
