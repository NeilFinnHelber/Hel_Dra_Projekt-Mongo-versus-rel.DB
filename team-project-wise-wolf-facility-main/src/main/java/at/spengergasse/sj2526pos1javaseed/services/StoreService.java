package at.spengergasse.sj2526pos1javaseed.services;

import at.spengergasse.sj2526pos1javaseed.Commands.CreateStoreCommand;
import at.spengergasse.sj2526pos1javaseed.Domain.Film;
import at.spengergasse.sj2526pos1javaseed.Domain.Series;
import at.spengergasse.sj2526pos1javaseed.Domain.Store;
import at.spengergasse.sj2526pos1javaseed.Ids.FilmId;
import at.spengergasse.sj2526pos1javaseed.Ids.SeriesId;
import at.spengergasse.sj2526pos1javaseed.Ids.StoreId;
import at.spengergasse.sj2526pos1javaseed.persistence.FilmRepository;
import at.spengergasse.sj2526pos1javaseed.persistence.SeriesRepository;
import at.spengergasse.sj2526pos1javaseed.persistence.StoreRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class StoreService {

    private final StoreRepository storeRepository;
    private final SeriesRepository seriesRepository;
    private final FilmRepository filmRepository;

    // neuen store anlegen, zugehörige filme und serien aus der db laden und verknüpfen
    @Transactional
    public Store create(@NotNull @Valid CreateStoreCommand command) {

        if (command.phoneNumber() == null) {
            throw new IllegalArgumentException("Phone number cannot be null");
        }

        Set<Series> seriesSet = new HashSet<>();
        if (command.seriesIds() != null) {
            seriesSet = command.seriesIds().stream()
                    .map(id -> seriesRepository.findById(new SeriesId(id))
                            .orElseThrow(() -> new IllegalArgumentException("Series not found: " + id)))
                    .collect(Collectors.toSet());
        }

        Set<Film> filmSet = new HashSet<>();
        if (command.filmIds() != null) {
            filmSet = command.filmIds().stream()
                    .map(id -> filmRepository.findById(new FilmId(id))
                            .orElseThrow(() -> new IllegalArgumentException("Film not found: " + id)))
                    .collect(Collectors.toSet());
        }

        Store store = Store.builder()
                .storeId(new StoreId(System.currentTimeMillis()))
                .phoneNumber(command.phoneNumber())
                .series(seriesSet)
                .films(filmSet)
                .build();

        return storeRepository.save(store);
    }

    // store direkt speichern
    public void saveStore(Store store) {
        storeRepository.save(store);
    }

    // bestehenden store aktualisieren
    public void updateStore(Store store) {
        storeRepository.save(store);
    }
    // store anhand der id löschen
    public void deleteStore(StoreId id) {
        storeRepository.deleteById(id);
    }

    // einen store anhand der id laden, fehler wenn nicht gefunden
    public Store getById(StoreId id) {
        return storeRepository.findById(id)
                .orElseThrow(() -> new java.util.NoSuchElementException("Store not found: " + id));
    }

    // alle stores als liste zurückgeben
    public java.util.List<Store> getAll() {
        return storeRepository.findAll();
    }

    // alle stores seitenweise zurückgeben
    public Page<Store> getAll(Pageable pageable) {
        return storeRepository.findAll(pageable);
    }
}
