package at.spengergasse.sj2526pos1javaseed.Commands;

import jakarta.validation.constraints.*;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
public record CreateOrderCommand(
        @NotNull LocalDateTime orderDate,
        @NotNull @DecimalMin("0.0") BigDecimal totalAmount,
        @NotNull Long storeId // wir wandeln im Service zu StoreId
) {}
