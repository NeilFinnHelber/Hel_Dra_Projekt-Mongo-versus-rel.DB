package at.spengergasse.sj2526pos1javaseed.serviceTests;

import at.spengergasse.sj2526pos1javaseed.Commands.CreateStoreCommand;
import at.spengergasse.sj2526pos1javaseed.Domain.Store;
import at.spengergasse.sj2526pos1javaseed.Ids.StoreId;
import at.spengergasse.sj2526pos1javaseed.persistence.FilmRepository;
import at.spengergasse.sj2526pos1javaseed.persistence.SeriesRepository;
import at.spengergasse.sj2526pos1javaseed.persistence.StoreRepository;
import at.spengergasse.sj2526pos1javaseed.richtypes.Phone_Number_Richtype;
import at.spengergasse.sj2526pos1javaseed.services.StoreService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class StoreServiceTest {

    @Mock private StoreRepository storeRepository;
    @Mock private SeriesRepository seriesRepository;
    @Mock private FilmRepository filmRepository;
    @InjectMocks private StoreService storeService;

    private Store buildStore(Long id) {
        return Store.builder().storeId(new StoreId(id))
                .phoneNumber(new Phone_Number_Richtype("123456789"))
                .films(new HashSet<>()).series(new HashSet<>()).build();
    }

    @Test
    void create_saves_store() {
        CreateStoreCommand cmd = new CreateStoreCommand(new Phone_Number_Richtype("123456789"), Set.of(), Set.of());
        when(storeRepository.save(any())).thenReturn(buildStore(1L));
        Store result = storeService.create(cmd);
        assertThat(result).isNotNull();
        verify(storeRepository).save(any());
    }

    @Test
    void create_throws_when_phone_null() {
        CreateStoreCommand cmd = new CreateStoreCommand(null, Set.of(), Set.of());
        assertThatThrownBy(() -> storeService.create(cmd)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void getById_returns_store() {
        when(storeRepository.findById(new StoreId(1L))).thenReturn(Optional.of(buildStore(1L)));
        Store result = storeService.getById(new StoreId(1L));
        assertThat(result).isNotNull();
    }

    @Test
    void getById_throws_when_not_found() {
        when(storeRepository.findById(any())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> storeService.getById(new StoreId(99L))).isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void getAll_returns_list() {
        when(storeRepository.findAll()).thenReturn(List.of(buildStore(1L), buildStore(2L)));
        assertThat(storeService.getAll()).hasSize(2);
    }

    @Test
    void getAll_paged_returns_page() {
        when(storeRepository.findAll(any(PageRequest.class))).thenReturn(new PageImpl<>(List.of(buildStore(1L))));
        Page<Store> result = storeService.getAll(PageRequest.of(0, 10));
        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void deleteStore_calls_deleteById() {
        storeService.deleteStore(new StoreId(1L));
        verify(storeRepository).deleteById(new StoreId(1L));
    }

    @Test
    void updateStore_calls_save() {
        Store store = buildStore(1L);
        storeService.updateStore(store);
        verify(storeRepository).save(store);
    }
}
