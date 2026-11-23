package at.spengergasse.sj2526pos1javaseed.Commands;

import at.spengergasse.sj2526pos1javaseed.Domain.Genre;
import at.spengergasse.sj2526pos1javaseed.Domain.Language;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.util.Set;

@Builder
public record CreateSeriesCommand(
        @NotBlank String title,
        @NotNull Integer releaseYear,
        @NotNull Integer episodes,
        @NotNull Set<Language> languages,
        @NotNull Float length,
        @NotNull Set<Genre> genres
) {}