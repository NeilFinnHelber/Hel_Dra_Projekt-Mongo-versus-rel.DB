package at.spengergasse.sj2526pos1javaseed.serviceTests;

import at.spengergasse.sj2526pos1javaseed.Commands.CreateOrderCommand;
import at.spengergasse.sj2526pos1javaseed.Domain.Order;
import at.spengergasse.sj2526pos1javaseed.Domain.Store;
import at.spengergasse.sj2526pos1javaseed.Ids.OrderId;
import at.spengergasse.sj2526pos1javaseed.Ids.StoreId;
import at.spengergasse.sj2526pos1javaseed.persistence.OrderRepository;
import at.spengergasse.sj2526pos1javaseed.persistence.StoreRepository;
import at.spengergasse.sj2526pos1javaseed.services.OrderService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private StoreRepository storeRepository;

    @InjectMocks
    private OrderService orderService;

    private Store buildStore() {
        return Store.builder().storeId(new StoreId(1L)).films(new HashSet<>()).series(new HashSet<>()).build();
    }

    private Order buildOrder(Long id) {
        return Order.builder()
                .id(new OrderId(id))
                .orderDate(LocalDateTime.now())
                .totalAmount(BigDecimal.valueOf(50.0))
                .store(buildStore())
                .build();
    }

    @Test
    void create_saves_and_returns_order() {
        CreateOrderCommand command = new CreateOrderCommand(LocalDateTime.now(), BigDecimal.valueOf(50.0), 1L);
        Order order = buildOrder(1L);
        when(storeRepository.findById(new StoreId(1L))).thenReturn(Optional.of(buildStore()));
        when(orderRepository.save(any())).thenReturn(order);

        Order result = orderService.create(command);

        assertThat(result).isNotNull();
        verify(orderRepository).save(any());
    }

    @Test
    void create_throws_when_store_not_found() {
        CreateOrderCommand command = new CreateOrderCommand(LocalDateTime.now(), BigDecimal.valueOf(50.0), 99L);
        when(storeRepository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.create(command))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void create_throws_when_total_amount_negative() {
        CreateOrderCommand command = new CreateOrderCommand(LocalDateTime.now(), BigDecimal.valueOf(-10.0), 1L);
        when(storeRepository.findById(new StoreId(1L))).thenReturn(Optional.of(buildStore()));

        assertThatThrownBy(() -> orderService.create(command))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void getById_returns_order_when_found() {
        Order order = buildOrder(1L);
        when(orderRepository.findById(new OrderId(1L))).thenReturn(Optional.of(order));

        Order result = orderService.getById(new OrderId(1L));

        assertThat(result.getTotalAmount()).isEqualByComparingTo(BigDecimal.valueOf(50.0));
    }

    @Test
    void getById_throws_when_not_found() {
        when(orderRepository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getById(new OrderId(99L)))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void getAll_returns_list() {
        when(orderRepository.findAll()).thenReturn(List.of(buildOrder(1L), buildOrder(2L)));

        List<Order> result = orderService.getAll();

        assertThat(result).hasSize(2);
    }

    @Test
    void getAll_paged_returns_page() {
        Page<Order> page = new PageImpl<>(List.of(buildOrder(1L)));
        when(orderRepository.findAll(any(PageRequest.class))).thenReturn(page);

        Page<Order> result = orderService.getAll(PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void deleteOrder_calls_deleteById() {
        orderService.deleteOrder(new OrderId(1L));
        verify(orderRepository).deleteById(new OrderId(1L));
    }
}
