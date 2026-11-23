package at.spengergasse.sj2526pos1javaseed.services;

import at.spengergasse.sj2526pos1javaseed.Commands.CreateSeriesCommand;
import at.spengergasse.sj2526pos1javaseed.Domain.Series;
import at.spengergasse.sj2526pos1javaseed.Ids.SeriesId;
import at.spengergasse.sj2526pos1javaseed.persistence.SeriesRepository;
import at.spengergasse.sj2526pos1javaseed.persistence.StoreRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@RequiredArgsConstructor

@Service
@Transactional(readOnly = true)
public class SeriesService {
    private final SeriesRepository seriesRepository;
    private final StoreRepository storeRepository;

    // neue serie aus dem command erstellen und speichern
    @Transactional
    public Series create(@NotNull @Valid CreateSeriesCommand command) {

        var series = Series.builder()
                .seriesid(new SeriesId(System.currentTimeMillis()))
                .title(command.title())
                .release_year(command.releaseYear())
                .episodes(command.episodes())
                .languages(command.languages())
                .length(command.length())
                .genres(command.genres())
                .build();

        return seriesRepository.save(series);
    }

    // serie direkt speichern
    public void saveSeries(Series series) {
        seriesRepository.save(series);
    }

    // bestehende serie aktualisieren
    @Transactional
    public void updateSeries(Series series) {
        seriesRepository.save(series);
    }

    // serie löschen: zuerst aus allen stores entfernen, dann aus der datenbank
    @Transactional
    public void deleteSeries(SeriesId id){
        var storesWithSeries = storeRepository.findAll().stream()
                .filter(store -> store.getSeries().stream().anyMatch(s -> s.getSeriesid().equals(id)))
                .toList();
        for (var store : storesWithSeries) {
            store.getSeries().removeIf(s -> s.getSeriesid().equals(id));
            storeRepository.save(store);
        }
        seriesRepository.deleteById(id);
    }

    // eine serie anhand der id suchen, fehler wenn nicht gefunden
    public Series getById(SeriesId id) {
        return seriesRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Series not found: " + id));
    }

    // alle serien als liste zurückgeben
    public List<Series> getAll() {
        return seriesRepository.findAll();
    }

    // alle serien seitenweise zurückgeben
    public Page<Series> getAll(Pageable pageable) {
        return seriesRepository.findAll(pageable);
    }
}