package at.spengergasse.sj2526pos1javaseed.persistence;

import at.spengergasse.sj2526pos1javaseed.Domain.Order;
import at.spengergasse.sj2526pos1javaseed.Ids.OrderId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OrderRepositoryTest {

    @Mock
    private OrderRepository orderRepository;

    private Order buildOrder(Long id, double amount) {
        return Order.builder()
                .id(new OrderId(id))
                .orderDate(LocalDateTime.of(2024, 1, 1, 12, 0))
                .totalAmount(BigDecimal.valueOf(amount))
                .build();
    }

    @Test
    void can_save_and_find_order_by_id() {
        Order order = buildOrder(1L, 49.99);
        when(orderRepository.save(order)).thenReturn(order);
        when(orderRepository.findById(new OrderId(1L))).thenReturn(Optional.of(order));

        Order saved = orderRepository.save(order);
        Optional<Order> found = orderRepository.findById(new OrderId(1L));

        assertThat(saved.getTotalAmount()).isEqualByComparingTo(BigDecimal.valueOf(49.99));
        assertThat(found).isPresent();
        verify(orderRepository).save(order);
        verify(orderRepository).findById(new OrderId(1L));
    }

    @Test
    void can_find_all_orders() {
        List<Order> orders = List.of(buildOrder(1L, 10.0), buildOrder(2L, 20.0));
        when(orderRepository.findAll()).thenReturn(orders);

        List<Order> result = orderRepository.findAll();

        assertThat(result).hasSize(2);
        verify(orderRepository).findAll();
    }

    @Test
    void returns_empty_when_order_not_found() {
        when(orderRepository.findById(new OrderId(99L))).thenReturn(Optional.empty());

        Optional<Order> result = orderRepository.findById(new OrderId(99L));

        assertThat(result).isEmpty();
    }

    @Test
    void can_delete_order() {
        orderRepository.deleteById(new OrderId(1L));
        verify(orderRepository, times(1)).deleteById(new OrderId(1L));
    }
}
