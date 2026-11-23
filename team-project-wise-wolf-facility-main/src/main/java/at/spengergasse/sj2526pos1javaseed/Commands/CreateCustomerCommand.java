package at.spengergasse.sj2526pos1javaseed.Commands;

import at.spengergasse.sj2526pos1javaseed.richtypes.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record CreateCustomerCommand(
        @NotBlank String firstName,
        @NotBlank String lastName,
        @NotNull Email email
) {}
