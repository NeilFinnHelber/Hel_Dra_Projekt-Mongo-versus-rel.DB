package at.spengergasse.sj2526pos1javaseed.serviceTests;

import at.spengergasse.sj2526pos1javaseed.Commands.CreateOrderPlacementCommand;
import at.spengergasse.sj2526pos1javaseed.Domain.*;
import at.spengergasse.sj2526pos1javaseed.Ids.*;
import at.spengergasse.sj2526pos1javaseed.persistence.CustomerRepository;
import at.spengergasse.sj2526pos1javaseed.persistence.Order_PlacementRepository;
import at.spengergasse.sj2526pos1javaseed.persistence.OrderRepository;
import at.spengergasse.sj2526pos1javaseed.richtypes.Email;
import at.spengergasse.sj2526pos1javaseed.services.OrderPlacementService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
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
public class OrderPlacementServiceTest {

    @Mock private Order_PlacementRepository orderPlacementRepository;
    @Mock private CustomerRepository customerRepository;
    @Mock private OrderRepository orderRepository;
    @InjectMocks private OrderPlacementService orderPlacementService;

    private Customer buildCustomer() {
        return Customer.builder().id(new UserId(1L)).nickname("u1").email(new Email("u1@x.com"))
                .permission(Permission.Customer).firstName("J").lastName("D").build();
    }

    private Order buildOrder() {
        return Order.builder().id(new OrderId(1L)).orderDate(LocalDateTime.now())
                .totalAmount(BigDecimal.TEN)
                .store(Store.builder().storeId(new StoreId(1L)).films(new HashSet<>()).series(new HashSet<>()).build())
                .build();
    }

    private Order_Placement buildPlacement(Long id) {
        return Order_Placement.builder().id(new Order_PlacementId(id))
                .orderDate(LocalDate.of(2024,1,1)).orderReturnDate(LocalDate.of(2024,1,15)).build();
    }

    @Test
    void create_saves_placement() {
        CreateOrderPlacementCommand cmd = new CreateOrderPlacementCommand(LocalDate.of(2024,1,1), LocalDate.of(2024,1,15), null, 1L, 1L);
        when(customerRepository.findById(new UserId(1L))).thenReturn(Optional.of(buildCustomer()));
        when(orderRepository.findById(new OrderId(1L))).thenReturn(Optional.of(buildOrder()));
        when(orderPlacementRepository.save(any())).thenReturn(buildPlacement(1L));
        Order_Placement result = orderPlacementService.create(cmd);
        assertThat(result).isNotNull();
        verify(orderPlacementRepository).save(any());
    }

    @Test
    void create_throws_when_customer_not_found() {
        CreateOrderPlacementCommand cmd = new CreateOrderPlacementCommand(LocalDate.now(), LocalDate.now().plusDays(1), null, 99L, 1L);
        when(customerRepository.findById(any())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> orderPlacementService.create(cmd)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void create_throws_when_return_before_order_date() {
        CreateOrderPlacementCommand cmd = new CreateOrderPlacementCommand(LocalDate.of(2024,1,15), LocalDate.of(2024,1,1), null, 1L, 1L);
        when(customerRepository.findById(any())).thenReturn(Optional.of(buildCustomer()));
        when(orderRepository.findById(any())).thenReturn(Optional.of(buildOrder()));
        assertThatThrownBy(() -> orderPlacementService.create(cmd)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void getById_returns_placement() {
        when(orderPlacementRepository.findById(new Order_PlacementId(1L))).thenReturn(Optional.of(buildPlacement(1L)));
        Order_Placement result = orderPlacementService.getById(new Order_PlacementId(1L));
        assertThat(result.getOrderDate()).isEqualTo(LocalDate.of(2024,1,1));
    }

    @Test
    void getById_throws_when_not_found() {
        when(orderPlacementRepository.findById(any())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> orderPlacementService.getById(new Order_PlacementId(99L))).isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void getAll_returns_list() {
        when(orderPlacementRepository.findAll()).thenReturn(List.of(buildPlacement(1L), buildPlacement(2L)));
        assertThat(orderPlacementService.getAll()).hasSize(2);
    }

    @Test
    void getAll_paged_returns_page() {
        when(orderPlacementRepository.findAll(any(PageRequest.class))).thenReturn(new PageImpl<>(List.of(buildPlacement(1L))));
        Page<Order_Placement> result = orderPlacementService.getAll(PageRequest.of(0,10));
        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void delete_calls_deleteById() {
        orderPlacementService.deleteOrderPlacement(new Order_PlacementId(1L));
        verify(orderPlacementRepository).deleteById(new Order_PlacementId(1L));
    }
}
