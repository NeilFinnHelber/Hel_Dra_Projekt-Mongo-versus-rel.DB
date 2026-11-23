package at.spengergasse.sj2526pos1javaseed.Commands;

import at.spengergasse.sj2526pos1javaseed.richtypes.Phone_Number_Richtype;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.util.Set;

@Builder
public record CreateStoreCommand(
        @NotNull Phone_Number_Richtype phoneNumber,
        Set<Long> seriesIds, // IDs von Series
        Set<Long> filmIds    // IDs von Film
) {}
