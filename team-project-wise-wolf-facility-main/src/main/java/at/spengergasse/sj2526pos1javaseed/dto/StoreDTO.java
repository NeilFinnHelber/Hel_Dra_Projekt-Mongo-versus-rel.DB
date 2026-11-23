package at.spengergasse.sj2526pos1javaseed.dto;

import at.spengergasse.sj2526pos1javaseed.Domain.Store;

import java.util.Set;
import java.util.stream.Collectors;

public record StoreDTO(
        Long id,
        String phoneNumber,
        Set<Long> filmIds,
        Set<Long> seriesIds
) {
    public static StoreDTO from(Store store) {
        return new StoreDTO(
                store.getStoreId() != null ? store.getStoreId().getId() : null,
                store.getPhoneNumber() != null ? store.getPhoneNumber().getValue() : null,
                store.getFilms() != null
                        ? store.getFilms().stream()
                            .map(f -> f.getFilmid() != null ? f.getFilmid().getId() : null)
                            .collect(Collectors.toSet())
                        : Set.of(),
                store.getSeries() != null
                        ? store.getSeries().stream()
                            .map(s -> s.getSeriesid() != null ? s.getSeriesid().getId() : null)
                            .collect(Collectors.toSet())
                        : Set.of()
        );
    }
}
