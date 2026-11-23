package at.spengergasse.sj2526pos1javaseed.dto;

import at.spengergasse.sj2526pos1javaseed.Domain.BillingInformation;

import java.time.LocalDate;

public record BillingInformationDTO(
        Long id,
        LocalDate expirationDate,
        String cardNumber,
        Long customerId
) {
    public static BillingInformationDTO from(BillingInformation bi) {
        return new BillingInformationDTO(
                bi.getId() != null ? bi.getId().getId() : null,
                bi.getExpirationDate(),
                bi.getCardNumber(),
                bi.getCustomer() != null && bi.getCustomer().getId() != null
                        ? bi.getCustomer().getId().getId() : null
        );
    }
}
