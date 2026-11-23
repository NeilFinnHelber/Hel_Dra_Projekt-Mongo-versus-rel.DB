package at.spengergasse.sj2526pos1javaseed.services;

import at.spengergasse.sj2526pos1javaseed.Commands.CreateOrderPlacementCommand;
import at.spengergasse.sj2526pos1javaseed.Domain.Customer;
import at.spengergasse.sj2526pos1javaseed.Domain.Order;
import at.spengergasse.sj2526pos1javaseed.Domain.Order_Placement;
import at.spengergasse.sj2526pos1javaseed.Ids.UserId;
import at.spengergasse.sj2526pos1javaseed.Ids.OrderId;
import at.spengergasse.sj2526pos1javaseed.Ids.Order_PlacementId;
import at.spengergasse.sj2526pos1javaseed.persistence.CustomerRepository;
import at.spengergasse.sj2526pos1javaseed.persistence.Order_PlacementRepository;
import at.spengergasse.sj2526pos1javaseed.persistence.OrderRepository;
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
public class OrderPlacementService {

    private final Order_PlacementRepository orderPlacementRepository;
    private final CustomerRepository customerRepository;
    private final OrderRepository orderRepository;

    // neue bestellplatzierung anlegen, kunden und bestellung verknüpfen
    @Transactional
    public Order_Placement create(@NotNull @Valid CreateOrderPlacementCommand command) {

        Customer customer = customerRepository.findById(new UserId(command.customerId()))
                .orElseThrow(() -> new IllegalArgumentException("Customer not found"));

        Order order = orderRepository.findById(new OrderId(command.orderId()))
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));

        if (command.orderDate() == null || command.orderReturnDate() == null ||
                command.orderReturnDate().isBefore(command.orderDate())) {
            throw new IllegalArgumentException("Invalid order placement dates");
        }

        Order_Placement placement = Order_Placement.builder()
                .id(new Order_PlacementId(System.currentTimeMillis())) // feste Zahl für Tests
                .orderDate(command.orderDate())
                .orderReturnDate(command.orderReturnDate())
                .actualReturnDate(command.actualReturnDate())
                .customer(customer)
                .order(order)
                .build();

        // Bidirektionale Beziehungen setzen
        placement.setCustomer(customer);
        placement.setOrder(order);

        return orderPlacementRepository.save(placement);
    }

    // bestellplatzierung anhand der id löschen
    public void deleteOrderPlacement(Order_PlacementId id){
        orderPlacementRepository.deleteById(id);
    }

    // bestellplatzierung direkt speichern
    public void saveOrderPlacement(Order_Placement placement){
        orderPlacementRepository.save(placement);
    }

    // bestehende bestellplatzierung aktualisieren
    public void updateOrderPlacement(Order_Placement orderPlacement){
        orderPlacementRepository.save(orderPlacement);
    }

    // eine bestellplatzierung anhand der id laden, fehler wenn nicht gefunden
    public Order_Placement getById(Order_PlacementId id) {
        return orderPlacementRepository.findById(id)
                .orElseThrow(() -> new java.util.NoSuchElementException("OrderPlacement not found: " + id));
    }

    // alle bestellplatzierungen als liste zurückgeben
    public java.util.List<Order_Placement> getAll() {
        return orderPlacementRepository.findAll();
    }

    // alle bestellplatzierungen seitenweise zurückgeben
    public Page<Order_Placement> getAll(Pageable pageable) {
        return orderPlacementRepository.findAll(pageable);
    }
}
