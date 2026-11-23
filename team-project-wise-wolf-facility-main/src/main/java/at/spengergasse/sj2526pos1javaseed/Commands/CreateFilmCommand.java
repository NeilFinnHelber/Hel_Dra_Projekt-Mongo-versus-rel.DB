package at.spengergasse.sj2526pos1javaseed.Commands;

import at.spengergasse.sj2526pos1javaseed.Domain.Genre;
import at.spengergasse.sj2526pos1javaseed.Domain.Language;
import at.spengergasse.sj2526pos1javaseed.Domain.User;
import at.spengergasse.sj2526pos1javaseed.Ids.FilmId;
import at.spengergasse.sj2526pos1javaseed.Ids.UserId;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.Set;

@Builder
public record CreateFilmCommand(
        @NotBlank String title,
        @NotNull Integer releaseYear,
        @NotNull Float length,
        @NotNull Set<Language> languages,
        @NotNull Set<Genre> genres

) {}

