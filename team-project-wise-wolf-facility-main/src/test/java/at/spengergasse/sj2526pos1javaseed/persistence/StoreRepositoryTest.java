package at.spengergasse.sj2526pos1javaseed.persistence;

import at.spengergasse.sj2526pos1javaseed.Domain.Store;
import at.spengergasse.sj2526pos1javaseed.Ids.StoreId;
import at.spengergasse.sj2526pos1javaseed.richtypes.Phone_Number_Richtype;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class StoreRepositoryTest {

    @Mock
    private StoreRepository storeRepository;

    private Store buildStore(Long id, String phone) {
        return Store.builder()
                .storeId(new StoreId(id))
                .phoneNumber(new Phone_Number_Richtype(phone))
                .films(new HashSet<>())
                .series(new HashSet<>())
                .build();
    }

    @Test
    void can_save_and_find_store_by_id() {
        Store store = buildStore(1L, "123456789");
        when(storeRepository.save(store)).thenReturn(store);
        when(storeRepository.findById(new StoreId(1L))).thenReturn(Optional.of(store));

        Store saved = storeRepository.save(store);
        Optional<Store> found = storeRepository.findById(new StoreId(1L));

        assertThat(saved.getPhoneNumber().getValue()).isEqualTo("123456789");
        assertThat(found).isPresent();
        verify(storeRepository).save(store);
        verify(storeRepository).findById(new StoreId(1L));
    }

    @Test
    void can_find_all_stores() {
        List<Store> stores = List.of(buildStore(1L, "111111111"), buildStore(2L, "222222222"));
        when(storeRepository.findAll()).thenReturn(stores);

        List<Store> result = storeRepository.findAll();

        assertThat(result).hasSize(2);
        verify(storeRepository).findAll();
    }

    @Test
    void returns_empty_when_store_not_found() {
        when(storeRepository.findById(new StoreId(99L))).thenReturn(Optional.empty());

        Optional<Store> result = storeRepository.findById(new StoreId(99L));

        assertThat(result).isEmpty();
    }

    @Test
    void can_delete_store() {
        storeRepository.deleteById(new StoreId(1L));
        verify(storeRepository, times(1)).deleteById(new StoreId(1L));
    }
}
