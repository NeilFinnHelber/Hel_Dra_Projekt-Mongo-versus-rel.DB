package at.spengergasse.sj2526pos1javaseed.dto;

import at.spengergasse.sj2526pos1javaseed.Domain.Order;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrderDTO(
        Long id,
        LocalDateTime orderDate,
        BigDecimal totalAmount,
        Long storeId
) {
    public static OrderDTO from(Order order) {
        return new OrderDTO(
                order.getId() != null ? order.getId().getId() : null,
                order.getOrderDate(),
                order.getTotalAmount(),
                order.getStore() != null && order.getStore().getStoreId() != null
                        ? order.getStore().getStoreId().getId() : null
        );
    }
}
