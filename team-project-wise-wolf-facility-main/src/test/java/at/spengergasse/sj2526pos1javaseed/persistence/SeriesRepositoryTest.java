package at.spengergasse.sj2526pos1javaseed.persistence;

import at.spengergasse.sj2526pos1javaseed.Domain.*;
import at.spengergasse.sj2526pos1javaseed.Ids.SeriesId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SeriesRepositoryTest {

    @Mock
    private SeriesRepository seriesRepository;

    private Series buildSeries(Long id, String title) {
        return Series.builder()
                .seriesid(new SeriesId(id))
                .title(title)
                .release_year(2008)
                .episodes(62)
                .length(47.0f)
                .languages(Set.of(Language.English))
                .genres(Set.of(Genre.Drama))
                .build();
    }

    @Test
    void can_save_and_find_series_by_id() {
        Series series = buildSeries(1L, "Breaking Bad");
        when(seriesRepository.save(series)).thenReturn(series);
        when(seriesRepository.findById(new SeriesId(1L))).thenReturn(Optional.of(series));

        Series saved = seriesRepository.save(series);
        Optional<Series> found = seriesRepository.findById(new SeriesId(1L));

        assertThat(saved.getTitle()).isEqualTo("Breaking Bad");
        assertThat(found).isPresent();
        assertThat(found.get().getTitle()).isEqualTo("Breaking Bad");
        verify(seriesRepository).save(series);
        verify(seriesRepository).findById(new SeriesId(1L));
    }

    @Test
    void can_find_all_series() {
        List<Series> seriesList = List.of(buildSeries(1L, "Series A"), buildSeries(2L, "Series B"));
        when(seriesRepository.findAll()).thenReturn(seriesList);

        List<Series> result = seriesRepository.findAll();

        assertThat(result).hasSize(2);
        verify(seriesRepository).findAll();
    }

    @Test
    void returns_empty_when_series_not_found() {
        when(seriesRepository.findById(new SeriesId(99L))).thenReturn(Optional.empty());

        Optional<Series> result = seriesRepository.findById(new SeriesId(99L));

        assertThat(result).isEmpty();
    }

    @Test
    void can_delete_series() {
        seriesRepository.deleteById(new SeriesId(1L));
        verify(seriesRepository, times(1)).deleteById(new SeriesId(1L));
    }
}
