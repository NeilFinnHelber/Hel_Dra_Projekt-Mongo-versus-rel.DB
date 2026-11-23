package at.spengergasse.sj2526pos1javaseed.presentation;

import at.spengergasse.sj2526pos1javaseed.Commands.CreateStoreCommand;
import at.spengergasse.sj2526pos1javaseed.Domain.Store;
import at.spengergasse.sj2526pos1javaseed.Ids.StoreId;
import at.spengergasse.sj2526pos1javaseed.richtypes.Phone_Number_Richtype;
import at.spengergasse.sj2526pos1javaseed.services.StoreService;
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
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = StoreController.class, excludeAutoConfiguration = {org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class, org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration.class})
public class StoreControllerTest {

    @MockitoBean
    private StoreService storeService;

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
    void getAllStores_returns_paged_DTO() throws Exception {
        Store store = Store.builder()
                .storeId(new StoreId(1L))
                .phoneNumber(Phone_Number_Richtype.of("123456789"))
                .build();

        Page<Store> page = new PageImpl<>(List.of(store));
        when(storeService.getAll(any(Pageable.class))).thenReturn(page);

        this.mockMvc.perform(get("/api/v1.0.0/stores")
                        .param("page", "0")
                        .param("size", "10")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.storeDTOList").isArray());

        verify(storeService, times(1)).getAll(any(Pageable.class));
    }

    @Test
    void getStore_returns_store() throws Exception {
        Store store = Store.builder()
                .storeId(new StoreId(1L))
                .phoneNumber(Phone_Number_Richtype.of("123456789"))
                .build();

        when(storeService.getById(new StoreId(1L))).thenReturn(store);

        this.mockMvc.perform(get("/api/v1.0.0/stores/1")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phoneNumber").value("123456789"));

        verify(storeService, times(1)).getById(new StoreId(1L));
    }

    @Test
    void getStore_returns_not_found() throws Exception {
        when(storeService.getById(new StoreId(1L))).thenThrow(new NoSuchElementException());

        this.mockMvc.perform(get("/api/v1.0.0/stores/1")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    void createStore_returns_created_store() throws Exception {
        CreateStoreCommand command = CreateStoreCommand.builder()
                .phoneNumber(Phone_Number_Richtype.of("123456789"))
                .seriesIds(Set.of())
                .filmIds(Set.of())
                .build();

        Store store = Store.builder()
                .storeId(new StoreId(1L))
                .phoneNumber(Phone_Number_Richtype.of("123456789"))
                .build();

        when(storeService.create(command)).thenReturn(store);

        this.mockMvc.perform(post("/api/v1.0.0/stores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phoneNumber").value("123456789"));

        verify(storeService, times(1)).create(command);
    }

    @Test
    void deleteStore_returns_no_content() throws Exception {
        this.mockMvc.perform(delete("/api/v1.0.0/stores/1"))
                .andExpect(status().isNoContent());

        verify(storeService, times(1)).deleteStore(new StoreId(1L));
    }
}
