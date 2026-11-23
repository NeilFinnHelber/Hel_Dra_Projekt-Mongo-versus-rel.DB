package at.spengergasse.sj2526pos1javaseed.presentation;

import at.spengergasse.sj2526pos1javaseed.Commands.CreateCustomerCommand;
import at.spengergasse.sj2526pos1javaseed.Domain.Customer;
import at.spengergasse.sj2526pos1javaseed.Ids.UserId;
import at.spengergasse.sj2526pos1javaseed.services.CustomerService;
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

@WebMvcTest(value = CustomerController.class, excludeAutoConfiguration = {org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class, org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration.class})
public class CustomerControllerTest {

    @MockitoBean
    private CustomerService customerService;

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
    void getAllCustomers_returns_paged_DTO() throws Exception {
        Customer customer = Customer.builder()
                .id(new UserId(1L))
                .firstName("John")
                .lastName("Doe")
                .build();

        Page<Customer> page = new PageImpl<>(List.of(customer));
        when(customerService.getAll(any(Pageable.class))).thenReturn(page);

        this.mockMvc.perform(get("/api/v1.0.0/customers")
                        .param("page", "0")
                        .param("size", "10")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.customerDTOList").isArray());

        verify(customerService, times(1)).getAll(any(Pageable.class));
    }

    @Test
    void getCustomer_returns_customer() throws Exception {
        Customer customer = Customer.builder()
                .id(new UserId(1L))
                .firstName("John")
                .lastName("Doe")
                .build();

        when(customerService.getById(new UserId(1L))).thenReturn(customer);

        this.mockMvc.perform(get("/api/v1.0.0/customers/1")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("John"));

        verify(customerService, times(1)).getById(new UserId(1L));
    }

    @Test
    void getCustomer_returns_not_found() throws Exception {
        when(customerService.getById(new UserId(1L))).thenThrow(new NoSuchElementException());

        this.mockMvc.perform(get("/api/v1.0.0/customers/1")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    void createCustomer_returns_created_customer() throws Exception {
        Customer customer = Customer.builder()
                .id(new UserId(1L))
                .firstName("John")
                .lastName("Doe")
                .build();

        when(customerService.create(any())).thenReturn(customer);

        String json = """
                {
                    "firstName": "John",
                    "lastName": "Doe",
                    "email": { "value": "test@test.com" }
                }
                """;

        this.mockMvc.perform(post("/api/v1.0.0/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("John"));

        verify(customerService, times(1)).create(any());
    }

    @Test
    void updateCustomer_returns_updated_customer() throws Exception {
        Customer customer = Customer.builder()
                .id(new UserId(1L))
                .firstName("Jane")
                .lastName("Doe")
                .build();

        when(customerService.getById(new UserId(1L))).thenReturn(customer);

        String json = """
                {
                    "firstName": "Jane",
                    "lastName": "Doe",
                    "email": { "value": "test@test.com" }
                }
                """;

        this.mockMvc.perform(patch("/api/v1.0.0/customers/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Jane"));

        verify(customerService, times(1)).updateCustomer(any(Customer.class));
    }

    @Test
    void deleteCustomer_returns_no_content() throws Exception {
        this.mockMvc.perform(delete("/api/v1.0.0/customers/1"))
                .andExpect(status().isNoContent());

        verify(customerService, times(1)).deleteCustomer(new UserId(1L));
    }
}
