package at.spengergasse.sj2526pos1javaseed.presentation;

import at.spengergasse.sj2526pos1javaseed.Domain.Order;
import at.spengergasse.sj2526pos1javaseed.Ids.OrderId;
import at.spengergasse.sj2526pos1javaseed.services.OrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;
import java.util.NoSuchElementException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = OrderController.class, excludeAutoConfiguration = {org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class, org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration.class})
public class OrderControllerTest {

    @MockitoBean
    private OrderService orderService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setup(WebApplicationContext webApplicationContext) {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .build();
    }

    @Test
    void getAllOrders_returns_paged_DTO() throws Exception {
        Order order = Order.builder()
                .id(new OrderId(1L))
                .build();

        Page<Order> page = new PageImpl<>(List.of(order));
        when(orderService.getAll(any(Pageable.class))).thenReturn(page);

        this.mockMvc.perform(get("/api/v1.0.0/orders")
                        .param("page", "0")
                        .param("size", "10")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.orderDTOList").isArray());

        verify(orderService, times(1)).getAll(any(Pageable.class));
    }

    @Test
    void getOrder_returns_order() throws Exception {
        Order order = Order.builder()
                .id(new OrderId(1L))
                .build();

        when(orderService.getById(new OrderId(1L))).thenReturn(order);

        this.mockMvc.perform(get("/api/v1.0.0/orders/1")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));

        verify(orderService, times(1)).getById(new OrderId(1L));
    }

    @Test
    void getOrder_returns_not_found() throws Exception {
        when(orderService.getById(new OrderId(1L))).thenThrow(new NoSuchElementException());

        this.mockMvc.perform(get("/api/v1.0.0/orders/1")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    void createOrder_returns_created_order() throws Exception {
        Order order = Order.builder()
                .id(new OrderId(1L))
                .build();

        when(orderService.create(any())).thenReturn(order);

        String json = """
                {
                    "orderDate": "2025-01-01T12:00:00",
                    "totalAmount": 100.50,
                    "storeId": 1
                }
                """;

        this.mockMvc.perform(post("/api/v1.0.0/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));

        verify(orderService, times(1)).create(any());
    }

    @Test
    void deleteOrder_returns_no_content() throws Exception {
        this.mockMvc.perform(delete("/api/v1.0.0/orders/1"))
                .andExpect(status().isNoContent());

        verify(orderService, times(1)).deleteOrder(new OrderId(1L));
    }
}
