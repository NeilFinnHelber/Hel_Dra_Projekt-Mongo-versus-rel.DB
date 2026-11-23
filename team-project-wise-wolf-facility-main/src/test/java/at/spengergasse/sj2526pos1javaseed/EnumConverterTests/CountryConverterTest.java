package at.spengergasse.sj2526pos1javaseed.EnumConverterTests;

import at.spengergasse.sj2526pos1javaseed.Domain.Country;
import at.spengergasse.sj2526pos1javaseed.persistence.converters.CountryConverter;
import at.spengergasse.sj2526pos1javaseed.persistence.converters.DataQualityException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class CountryConverterTest {

    private final CountryConverter converter = new CountryConverter();

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
    void can_convert_to_dbvalue_and_back(Country country, String dbValue) {
        String convertedDbValue = converter.convertToDatabaseColumn(country);

        assertThat(convertedDbValue).isEqualTo(dbValue);
        assertThat(converter.convertToEntityAttribute(convertedDbValue)).isEqualTo(country);
    }

    private static Stream<Arguments> mappingData() {
        return Stream.of(
                Arguments.of(Country.USA, "U"),
                Arguments.of(Country.Italy, "I"),
                Arguments.of(Country.Japan, "J"),
                Arguments.of(Country.Germany, "GER"),
                Arguments.of(Country.Spain, "SP"),
                Arguments.of(Country.France, "F"),
                Arguments.of(Country.Greece, "GR"),
                Arguments.of(Country.Austria, "A"),
                Arguments.of(Country.Switzerland, "SW")
        );
    }
}
