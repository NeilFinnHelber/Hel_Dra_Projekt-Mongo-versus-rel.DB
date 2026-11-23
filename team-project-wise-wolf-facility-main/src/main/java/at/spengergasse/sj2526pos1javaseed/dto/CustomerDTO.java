package at.spengergasse.sj2526pos1javaseed.dto;

import at.spengergasse.sj2526pos1javaseed.Domain.Customer;

public record CustomerDTO(
        Long id,
        String firstName,
        String lastName,
        String email,
        String nickname
) {
    public static CustomerDTO from(Customer customer) {
        return new CustomerDTO(
                customer.getId() != null ? customer.getId().getId() : null,
                customer.getFirstName(),
                customer.getLastName(),
                customer.getEmail() != null ? customer.getEmail().getValue() : null,
                customer.getNickname()
        );
    }
}
