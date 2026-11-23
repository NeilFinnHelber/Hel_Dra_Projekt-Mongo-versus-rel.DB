package at.spengergasse.sj2526pos1javaseed.persistence.converters;

import at.spengergasse.sj2526pos1javaseed.richtypes.Phone_Number_Richtype;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class PhoneNumberConverterTest {

    private final PhoneNumberConverter converter = new PhoneNumberConverter();

    @Test
    void can_convert_null_safe() {
        assertThat(converter.convertToDatabaseColumn(null)).isNull();
        assertThat(converter.convertToEntityAttribute(null)).isNull();
        assertThat(converter.convertToEntityAttribute("   ")).isNull();
    }

    @Test
    void can_convert_to_dbvalue_and_back() {
        Phone_Number_Richtype phone = new Phone_Number_Richtype("123456789");
        String dbValue = converter.convertToDatabaseColumn(phone);

        assertThat(dbValue).isEqualTo("123456789");
        assertThat(converter.convertToEntityAttribute(dbValue)).isEqualTo(phone);
    }
}
