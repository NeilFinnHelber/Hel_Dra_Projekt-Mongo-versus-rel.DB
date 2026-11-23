package at.spengergasse.sj2526pos1javaseed.Commands;

import jakarta.validation.constraints.*;
import lombok.Builder;

import java.time.LocalDate;

@Builder
public record CreateBillingInformationCommand(
        @NotNull LocalDate expirationDate,
        @NotBlank String cardNumber,
        @Min(100) @Max(9999) int cvv, // einfacher CVV Check
        @NotNull Long customerId
) {}
