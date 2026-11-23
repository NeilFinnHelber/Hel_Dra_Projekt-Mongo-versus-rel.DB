package at.spengergasse.sj2526pos1javaseed.EnumConverterTests;

import at.spengergasse.sj2526pos1javaseed.Domain.Language;
import at.spengergasse.sj2526pos1javaseed.persistence.converters.DataQualityException;
import at.spengergasse.sj2526pos1javaseed.persistence.converters.LanguageConverter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class LanguageConverterTest {

    private final LanguageConverter converter = new LanguageConverter();

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
    void can_convert_to_dbvalue_and_back(Language language, String dbValue) {
        String convertedDbValue = converter.convertToDatabaseColumn(language);

        assertThat(convertedDbValue).isEqualTo(dbValue);
        assertThat(converter.convertToEntityAttribute(convertedDbValue)).isEqualTo(language);
    }

    private static Stream<Arguments> mappingData() {
        return Stream.of(
                Arguments.of(Language.French, "F"),
                Arguments.of(Language.English, "E"),
                Arguments.of(Language.Spanish, "S"),
                Arguments.of(Language.German, "G"),
                Arguments.of(Language.Japanese, "J")
        );
    }
}
