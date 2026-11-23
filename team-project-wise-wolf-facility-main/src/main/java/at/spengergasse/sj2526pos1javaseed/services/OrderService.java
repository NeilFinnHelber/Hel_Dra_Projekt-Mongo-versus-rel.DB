package at.spengergasse.sj2526pos1javaseed.services;

import at.spengergasse.sj2526pos1javaseed.Commands.CreateOrderCommand;
import at.spengergasse.sj2526pos1javaseed.Domain.Order;
import at.spengergasse.sj2526pos1javaseed.Domain.Store;
import at.spengergasse.sj2526pos1javaseed.Ids.OrderId;
import at.spengergasse.sj2526pos1javaseed.Ids.StoreId;
import at.spengergasse.sj2526pos1javaseed.persistence.OrderRepository;
import at.spengergasse.sj2526pos1javaseed.persistence.StoreRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final StoreRepository storeRepository;

    // neue bestellung anlegen, store wird anhand der id gesucht
    @Transactional
    public Order create(@NotNull @Valid CreateOrderCommand command) {

        Store store = storeRepository.findById(new StoreId(command.storeId()))
                .orElseThrow(() -> new IllegalArgumentException("Store not found"));

        if (command.totalAmount().compareTo(BigDecimal.ZERO) < 0 || command.orderDate() == null) {
            throw new IllegalArgumentException("Invalid order data");
        }

        Order order = Order.builder()
                .id(new OrderId(System.currentTimeMillis())) // feste Zahl/Long für Tests
                .orderDate(command.orderDate())
                .totalAmount(command.totalAmount())
                .store(store)
                .build();

        return orderRepository.save(order);
    }

    // bestellung löschen – entity zuerst laden damit cascades korrekt ausgeführt werden
    public void deleteOrder(OrderId id) {
        orderRepository.findById(id).ifPresent(orderRepository::delete);
    }

    // bestellung direkt speichern
    public void saveOrder(Order order){
        orderRepository.save(order);
    }

    // bestehende bestellung aktualisieren
    public void updateOrder(Order order){
        orderRepository.save(order);
    }

    // eine bestellung anhand der id laden, fehler wenn nicht gefunden
    public Order getById(OrderId id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new java.util.NoSuchElementException("Order not found: " + id));
    }

    // alle bestellungen als liste zurückgeben
    public java.util.List<Order> getAll() {
        return orderRepository.findAll();
    }

    // alle bestellungen seitenweise zurückgeben
    public Page<Order> getAll(Pageable pageable) {
        return orderRepository.findAll(pageable);
    }
}
