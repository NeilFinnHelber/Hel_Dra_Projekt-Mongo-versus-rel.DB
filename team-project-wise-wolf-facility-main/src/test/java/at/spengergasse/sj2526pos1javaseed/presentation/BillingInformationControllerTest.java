package at.spengergasse.sj2526pos1javaseed.presentation;

import at.spengergasse.sj2526pos1javaseed.Commands.CreateBillingInformationCommand;
import at.spengergasse.sj2526pos1javaseed.Domain.BillingInformation;
import at.spengergasse.sj2526pos1javaseed.Domain.Customer;
import at.spengergasse.sj2526pos1javaseed.Ids.BillingInformationId;
import at.spengergasse.sj2526pos1javaseed.Ids.UserId;
import at.spengergasse.sj2526pos1javaseed.services.BillingInformationService;
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

import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = BillingInformationController.class, excludeAutoConfiguration = {org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class, org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration.class})
public class BillingInformationControllerTest {

    @MockitoBean
    private BillingInformationService billingInformationService;

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
    void getAllBilling_returns_paged_DTO() throws Exception {
        Customer customer = mock(Customer.class);
        when(customer.getId()).thenReturn(new UserId(1L));

        BillingInformation bi = BillingInformation.builder()
                .id(new BillingInformationId(1L))
                .cardNumber("1234123412341234")
                .cvv(123)
                .expirationDate(LocalDate.now().plusDays(10))
                .customer(customer)
                .build();

        Page<BillingInformation> page = new PageImpl<>(List.of(bi));
        when(billingInformationService.getAll(any(Pageable.class))).thenReturn(page);

        this.mockMvc.perform(get("/api/v1.0.0/billing")
                        .param("page", "0")
                        .param("size", "10")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.billingInformationDTOList").isArray());

        verify(billingInformationService, times(1)).getAll(any(Pageable.class));
    }

    @Test
    void getBilling_returns_billing() throws Exception {
        Customer customer = mock(Customer.class);
        when(customer.getId()).thenReturn(new UserId(1L));

        BillingInformation bi = BillingInformation.builder()
                .id(new BillingInformationId(1L))
                .cardNumber("1234123412341234")
                .cvv(123)
                .expirationDate(LocalDate.now().plusDays(10))
                .customer(customer)
                .build();

        when(billingInformationService.getById(new BillingInformationId(1L))).thenReturn(bi);

        this.mockMvc.perform(get("/api/v1.0.0/billing/1")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cardNumber").value("1234123412341234"));

        verify(billingInformationService, times(1)).getById(new BillingInformationId(1L));
    }

    @Test
    void getBilling_returns_not_found() throws Exception {
        when(billingInformationService.getById(new BillingInformationId(1L))).thenThrow(new NoSuchElementException());

        this.mockMvc.perform(get("/api/v1.0.0/billing/1")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    void createBilling_returns_created_billing() throws Exception {
        CreateBillingInformationCommand command = CreateBillingInformationCommand.builder()
                .cardNumber("1234123412341234")
                .cvv(123)
                .expirationDate(LocalDate.now().plusDays(10))
                .customerId(1L)
                .build();

        Customer customer = mock(Customer.class);
        when(customer.getId()).thenReturn(new UserId(1L));

        BillingInformation bi = BillingInformation.builder()
                .id(new BillingInformationId(1L))
                .cardNumber("1234123412341234")
                .cvv(123)
                .expirationDate(LocalDate.now().plusDays(10))
                .customer(customer)
                .build();

        when(billingInformationService.create(command)).thenReturn(bi);

        this.mockMvc.perform(post("/api/v1.0.0/billing")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cardNumber").value("1234123412341234"));

        verify(billingInformationService, times(1)).create(command);
    }

    @Test
    void deleteBilling_returns_no_content() throws Exception {
        this.mockMvc.perform(delete("/api/v1.0.0/billing/1"))
                .andExpect(status().isNoContent());

        verify(billingInformationService, times(1)).deleteBillingInformation(new BillingInformationId(1L));
    }
}
