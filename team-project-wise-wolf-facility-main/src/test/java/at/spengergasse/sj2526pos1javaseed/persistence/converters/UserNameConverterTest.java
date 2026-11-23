package at.spengergasse.sj2526pos1javaseed.persistence.converters;

import at.spengergasse.sj2526pos1javaseed.richtypes.UserName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class UserNameConverterTest {

    private final UserNameConverter converter = new UserNameConverter();

    @Test
    void can_convert_null_safe() {
        assertThat(converter.convertToDatabaseColumn(null)).isNull();
        assertThat(converter.convertToEntityAttribute(null)).isNull();
        assertThat(converter.convertToEntityAttribute("   ")).isNull();
    }

    @Test
    void can_convert_to_dbvalue_and_back() {
        UserName name = new UserName("john_doe123");
        String dbValue = converter.convertToDatabaseColumn(name);

        assertThat(dbValue).isEqualTo("john_doe123");
        assertThat(converter.convertToEntityAttribute(dbValue)).isEqualTo(name);
    }
}
