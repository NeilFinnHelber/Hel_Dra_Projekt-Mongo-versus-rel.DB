package at.spengergasse.sj2526pos1javaseed.EnumConverterTests;

import at.spengergasse.sj2526pos1javaseed.Domain.Permission;
import at.spengergasse.sj2526pos1javaseed.persistence.converters.DataQualityException;
import at.spengergasse.sj2526pos1javaseed.persistence.converters.PermissionConverter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class PermissionConverterTest {

    private final PermissionConverter converter = new PermissionConverter();

    @Test
    void can_convert_null_safe() {
        assertThat(converter.convertToEntityAttribute(null)).isNull();
        assertThat(converter.convertToDatabaseColumn(null)).isNull();
    }

    @Test
    void will_throw_data_quality_exception_for_unknown_dbvalue() {
        assertThatThrownBy(() -> converter.convertToEntityAttribute('X'))
                .isInstanceOf(DataQualityException.class);
    }

    @ParameterizedTest
    @MethodSource("mappingData")
    void can_convert_to_dbvalue_and_back(Permission permission, Character dbValue) {
        Character convertedDbValue = converter.convertToDatabaseColumn(permission);

        assertThat(convertedDbValue).isEqualTo(dbValue);
        assertThat(converter.convertToEntityAttribute(convertedDbValue)).isEqualTo(permission);
    }

    private static Stream<Arguments> mappingData() {
        return Stream.of(
                Arguments.of(Permission.Admin, 'A'),
                Arguments.of(Permission.Customer, 'C'),
                Arguments.of(Permission.Employee, 'E')
        );
    }
}
