package at.spengergasse.sj2526pos1javaseed.services;

import at.spengergasse.sj2526pos1javaseed.Commands.CreateBillingInformationCommand;
import at.spengergasse.sj2526pos1javaseed.Domain.BillingInformation;
import at.spengergasse.sj2526pos1javaseed.Domain.Customer;
import at.spengergasse.sj2526pos1javaseed.Ids.BillingInformationId;
import at.spengergasse.sj2526pos1javaseed.Ids.UserId;
import at.spengergasse.sj2526pos1javaseed.persistence.BillingInformationRepository;
import at.spengergasse.sj2526pos1javaseed.persistence.CustomerRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class BillingInformationService {

    private final BillingInformationRepository billingInformationRepository;
    private final CustomerRepository customerRepository;

    // neue zahlungsinformation anlegen, kunden aus der db laden und validieren
    @Transactional
    public BillingInformation create(@NotNull @Valid CreateBillingInformationCommand command) {

        // Hier Long in UserId konvertieren
        UserId customerId = new UserId(command.customerId());

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found"));

        if (command.cardNumber().isBlank() || command.expirationDate().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Invalid billing information");
        }

        BillingInformation billingInformation = BillingInformation.builder()
                .id(new BillingInformationId(5))
                .cardNumber(command.cardNumber())
                .cvv(command.cvv())
                .expirationDate(command.expirationDate())
                .customer(customer)
                .build();

        customer.getBillingInformation().add(billingInformation);

        return billingInformationRepository.save(billingInformation);
    }

    // zahlungsinformation direkt speichern
    public void saveBillingInformation(BillingInformation billingInformation) {
        billingInformationRepository.save(billingInformation);
    }

    // bestehende zahlungsinformation aktualisieren
    public void updateBillingInformation(BillingInformation billingInformation) {
        billingInformationRepository.save(billingInformation);
    }

    // zahlungsinformation anhand der id löschen
    public void deleteBillingInformation(BillingInformationId id) {
        billingInformationRepository.deleteById(id);
    }

    // eine zahlungsinformation anhand der id laden, fehler wenn nicht gefunden
    public BillingInformation getById(BillingInformationId id) {
        return billingInformationRepository.findById(id)
                .orElseThrow(() -> new java.util.NoSuchElementException("BillingInformation not found: " + id));
    }

    // alle zahlungsinformationen als liste zurückgeben
    public java.util.List<BillingInformation> getAll() {
        return billingInformationRepository.findAll();
    }

    // alle zahlungsinformationen seitenweise zurückgeben
    public Page<BillingInformation> getAll(Pageable pageable) {
        return billingInformationRepository.findAll(pageable);
    }
}
