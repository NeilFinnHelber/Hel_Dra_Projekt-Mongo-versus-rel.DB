package at.spengergasse.sj2526pos1javaseed.presentation;

import at.spengergasse.sj2526pos1javaseed.Domain.Order_Placement;
import at.spengergasse.sj2526pos1javaseed.Ids.Order_PlacementId;
import at.spengergasse.sj2526pos1javaseed.services.OrderPlacementService;
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

@WebMvcTest(value = OrderPlacementController.class, excludeAutoConfiguration = {org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class, org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration.class})
public class OrderPlacementControllerTest {

    @MockitoBean
    private OrderPlacementService orderPlacementService;

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
    void getAllOrderPlacements_returns_paged_DTO() throws Exception {
        Order_Placement placement = Order_Placement.builder()
                .id(new Order_PlacementId(1L))
                .build();

        Page<Order_Placement> page = new PageImpl<>(List.of(placement));
        when(orderPlacementService.getAll(any(Pageable.class))).thenReturn(page);

        this.mockMvc.perform(get("/api/v1.0.0/order-placements")
                        .param("page", "0")
                        .param("size", "10")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.orderPlacementDTOList").isArray());

        verify(orderPlacementService, times(1)).getAll(any(Pageable.class));
    }

    @Test
    void getOrderPlacement_returns_placement() throws Exception {
        Order_Placement placement = Order_Placement.builder()
                .id(new Order_PlacementId(1L))
                .build();

        when(orderPlacementService.getById(new Order_PlacementId(1L))).thenReturn(placement);

        this.mockMvc.perform(get("/api/v1.0.0/order-placements/1")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));

        verify(orderPlacementService, times(1)).getById(new Order_PlacementId(1L));
    }

    @Test
    void getOrderPlacement_returns_not_found() throws Exception {
        when(orderPlacementService.getById(new Order_PlacementId(1L))).thenThrow(new NoSuchElementException());

        this.mockMvc.perform(get("/api/v1.0.0/order-placements/1")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    void createOrderPlacement_returns_created_placement() throws Exception {
        Order_Placement placement = Order_Placement.builder()
                .id(new Order_PlacementId(1L))
                .build();

        when(orderPlacementService.create(any())).thenReturn(placement);

        String json = """
                {
                    "orderDate": "2025-01-01",
                    "orderReturnDate": "2025-01-10",
                    "customerId": 1,
                    "orderId": 1
                }
                """;

        this.mockMvc.perform(post("/api/v1.0.0/order-placements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));

        verify(orderPlacementService, times(1)).create(any());
    }

    @Test
    void deleteOrderPlacement_returns_no_content() throws Exception {
        this.mockMvc.perform(delete("/api/v1.0.0/order-placements/1"))
                .andExpect(status().isNoContent());

        verify(orderPlacementService, times(1)).deleteOrderPlacement(new Order_PlacementId(1L));
    }
}
