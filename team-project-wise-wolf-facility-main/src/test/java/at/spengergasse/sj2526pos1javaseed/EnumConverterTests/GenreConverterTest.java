package at.spengergasse.sj2526pos1javaseed.EnumConverterTests;

import at.spengergasse.sj2526pos1javaseed.Domain.Genre;
import at.spengergasse.sj2526pos1javaseed.persistence.converters.DataQualityException;
import at.spengergasse.sj2526pos1javaseed.persistence.converters.GenreConverter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class GenreConverterTest {

    private final GenreConverter converter = new GenreConverter();

    @Test
    void can_convert_null_safe() {
        assertThat(converter.convertToEntityAttribute(null)).isNull();
        assertThat(converter.convertToDatabaseColumn(null)).isNull();
    }

    @Test
    void will_throw_data_quality_exception_for_unknown_dbvalue() {
        assertThatThrownBy(() -> converter.convertToEntityAttribute("XYZ"))
                .isInstanceOf(DataQualityException.class);
    }

    @ParameterizedTest
    @MethodSource("mappingData")
    void can_convert_to_dbvalue_and_back(Genre genre, String dbValue) {
        String convertedDbValue = converter.convertToDatabaseColumn(genre);

        assertThat(convertedDbValue).isEqualTo(dbValue);
        assertThat(converter.convertToEntityAttribute(convertedDbValue)).isEqualTo(genre);
    }

    private static Stream<Arguments> mappingData() {
        return Stream.of(
                Arguments.of(Genre.Fantasy, "F"),
                Arguments.of(Genre.Crime, "CR"),
                Arguments.of(Genre.Drama, "DR"),
                Arguments.of(Genre.SciFi, "S"),
                Arguments.of(Genre.Action, "A"),
                Arguments.of(Genre.Comedy, "CO"),
                Arguments.of(Genre.Horror, "HO"),
                Arguments.of(Genre.Mystery, "M"),
                Arguments.of(Genre.Romance, "R"),
                Arguments.of(Genre.Western, "W"),
                Arguments.of(Genre.Thriller, "T"),
                Arguments.of(Genre.Adventure, "AD"),
                Arguments.of(Genre.Animation, "AN"),
                Arguments.of(Genre.Historical, "HI"),
                Arguments.of(Genre.Documentary, "DO")
        );
    }
}
