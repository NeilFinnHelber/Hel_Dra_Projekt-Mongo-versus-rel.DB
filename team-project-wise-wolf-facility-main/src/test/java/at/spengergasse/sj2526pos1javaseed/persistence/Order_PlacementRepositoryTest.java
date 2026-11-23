package at.spengergasse.sj2526pos1javaseed.persistence;

import at.spengergasse.sj2526pos1javaseed.Domain.Order_Placement;
import at.spengergasse.sj2526pos1javaseed.Ids.Order_PlacementId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class Order_PlacementRepositoryTest {

    @Mock
    private Order_PlacementRepository order_PlacementRepository;

    private Order_Placement buildPlacement(Long id) {
        return Order_Placement.builder()
                .id(new Order_PlacementId(id))
                .orderDate(LocalDate.of(2024, 1, 1))
                .orderReturnDate(LocalDate.of(2024, 1, 15))
                .build();
    }

    @Test
    void can_save_and_find_order_placement_by_id() {
        Order_Placement placement = buildPlacement(1L);
        when(order_PlacementRepository.save(placement)).thenReturn(placement);
        when(order_PlacementRepository.findById(new Order_PlacementId(1L))).thenReturn(Optional.of(placement));

        Order_Placement saved = order_PlacementRepository.save(placement);
        Optional<Order_Placement> found = order_PlacementRepository.findById(new Order_PlacementId(1L));

        assertThat(saved.getOrderDate()).isEqualTo(LocalDate.of(2024, 1, 1));
        assertThat(found).isPresent();
        verify(order_PlacementRepository).save(placement);
        verify(order_PlacementRepository).findById(new Order_PlacementId(1L));
    }

    @Test
    void can_find_all_order_placements() {
        List<Order_Placement> placements = List.of(buildPlacement(1L), buildPlacement(2L));
        when(order_PlacementRepository.findAll()).thenReturn(placements);

        List<Order_Placement> result = order_PlacementRepository.findAll();

        assertThat(result).hasSize(2);
        verify(order_PlacementRepository).findAll();
    }

    @Test
    void returns_empty_when_order_placement_not_found() {
        when(order_PlacementRepository.findById(new Order_PlacementId(99L))).thenReturn(Optional.empty());

        Optional<Order_Placement> result = order_PlacementRepository.findById(new Order_PlacementId(99L));

        assertThat(result).isEmpty();
    }

    @Test
    void can_delete_order_placement() {
        order_PlacementRepository.deleteById(new Order_PlacementId(1L));
        verify(order_PlacementRepository, times(1)).deleteById(new Order_PlacementId(1L));
    }
}
