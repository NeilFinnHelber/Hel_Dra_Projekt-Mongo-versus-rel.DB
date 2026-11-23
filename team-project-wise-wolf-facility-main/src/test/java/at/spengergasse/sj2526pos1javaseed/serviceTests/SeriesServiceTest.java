package at.spengergasse.sj2526pos1javaseed.serviceTests;

import at.spengergasse.sj2526pos1javaseed.Commands.CreateSeriesCommand;
import at.spengergasse.sj2526pos1javaseed.Domain.Genre;
import at.spengergasse.sj2526pos1javaseed.Domain.Language;
import at.spengergasse.sj2526pos1javaseed.Domain.Series;
import at.spengergasse.sj2526pos1javaseed.Ids.SeriesId;
import at.spengergasse.sj2526pos1javaseed.persistence.SeriesRepository;
import at.spengergasse.sj2526pos1javaseed.persistence.StoreRepository;
import at.spengergasse.sj2526pos1javaseed.services.SeriesService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SeriesServiceTest {

    @Mock
    private SeriesRepository seriesRepository;

    @Mock
    private StoreRepository storeRepository;

    @InjectMocks
    private SeriesService seriesService;

    private Series buildSeries(Long id) {
        return Series.builder()
                .seriesid(new SeriesId(id))
                .title("Series " + id)
                .release_year(2020)
                .episodes(10)
                .length(45.0f)
                .languages(Set.of(Language.English))
                .genres(Set.of(Genre.Drama))
                .build();
    }

    @Test
    void create_saves_and_returns_series() {
        CreateSeriesCommand command = new CreateSeriesCommand("Breaking Bad", 2008, 62, Set.of(Language.English), 47.0f, Set.of(Genre.Drama));
        Series series = buildSeries(1L);
        when(seriesRepository.save(any())).thenReturn(series);

        Series result = seriesService.create(command);

        assertThat(result).isNotNull();
        verify(seriesRepository).save(any());
    }

    @Test
    void getById_returns_series_when_found() {
        Series series = buildSeries(1L);
        when(seriesRepository.findById(new SeriesId(1L))).thenReturn(Optional.of(series));

        Series result = seriesService.getById(new SeriesId(1L));

        assertThat(result.getTitle()).isEqualTo("Series 1");
    }

    @Test
    void getById_throws_when_not_found() {
        when(seriesRepository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> seriesService.getById(new SeriesId(99L)))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void getAll_returns_list() {
        when(seriesRepository.findAll()).thenReturn(List.of(buildSeries(1L), buildSeries(2L)));

        List<Series> result = seriesService.getAll();

        assertThat(result).hasSize(2);
    }

    @Test
    void getAll_paged_returns_page() {
        Page<Series> page = new PageImpl<>(List.of(buildSeries(1L)));
        when(seriesRepository.findAll(any(PageRequest.class))).thenReturn(page);

        Page<Series> result = seriesService.getAll(PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void updateSeries_calls_save() {
        Series series = buildSeries(1L);
        seriesService.updateSeries(series);
        verify(seriesRepository).save(series);
    }

    @Test
    void deleteSeries_removes_from_stores_and_deletes() {
        when(storeRepository.findAll()).thenReturn(List.of());

        seriesService.deleteSeries(new SeriesId(1L));

        verify(seriesRepository).deleteById(new SeriesId(1L));
    }
}
