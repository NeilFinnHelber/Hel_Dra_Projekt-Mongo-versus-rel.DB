package at.spengergasse.sj2526pos1javaseed.persistence.converters;

import at.spengergasse.sj2526pos1javaseed.richtypes.Email;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class EmailConverterTest {

    private final EmailConverter converter = new EmailConverter();

    @Test
    void can_convert_null_safe() {
        assertThat(converter.convertToDatabaseColumn(null)).isNull();
        assertThat(converter.convertToEntityAttribute(null)).isNull();
    }

    @Test
    void can_convert_to_dbvalue_and_back() {
        Email email = new Email("test@example.com");
        String dbValue = converter.convertToDatabaseColumn(email);

        assertThat(dbValue).isEqualTo("test@example.com");
        assertThat(converter.convertToEntityAttribute(dbValue)).isEqualTo(email);
    }
}
