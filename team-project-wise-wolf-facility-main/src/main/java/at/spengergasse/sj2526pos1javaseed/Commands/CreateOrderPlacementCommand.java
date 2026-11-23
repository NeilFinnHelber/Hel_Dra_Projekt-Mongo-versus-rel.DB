package at.spengergasse.sj2526pos1javaseed.Commands;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.time.LocalDate;

@Builder
public record CreateOrderPlacementCommand(
        @NotNull LocalDate orderDate,
        @NotNull LocalDate orderReturnDate,
        LocalDate actualReturnDate,
        @NotNull Long customerId, // wird im Service zu CustomerId
        @NotNull Long orderId     // wird im Service zu OrderId
) {}
