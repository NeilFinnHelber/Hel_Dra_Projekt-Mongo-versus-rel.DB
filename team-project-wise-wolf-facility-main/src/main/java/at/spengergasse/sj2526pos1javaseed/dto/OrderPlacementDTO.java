package at.spengergasse.sj2526pos1javaseed.dto;

import at.spengergasse.sj2526pos1javaseed.Domain.Order_Placement;

import java.time.LocalDate;

public record OrderPlacementDTO(
        Long id,
        LocalDate orderDate,
        LocalDate orderReturnDate,
        LocalDate actualReturnDate,
        Long customerId,
        Long orderId
) {
    public static OrderPlacementDTO from(Order_Placement placement) {
        return new OrderPlacementDTO(
                placement.getId() != null ? placement.getId().getId() : null,
                placement.getOrderDate(),
                placement.getOrderReturnDate(),
                placement.getActualReturnDate(),
                placement.getCustomer() != null && placement.getCustomer().getId() != null
                        ? placement.getCustomer().getId().getId() : null,
                placement.getOrder() != null && placement.getOrder().getId() != null
                        ? placement.getOrder().getId().getId() : null
        );
    }
}
