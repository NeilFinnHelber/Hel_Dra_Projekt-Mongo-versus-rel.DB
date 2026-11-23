package at.spengergasse.sj2526pos1javaseed.persistence.converters;

import at.spengergasse.sj2526pos1javaseed.richtypes.Phone_Number_Richtype;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class PhoneNumberConverter implements AttributeConverter<Phone_Number_Richtype, String> {

    @Override
    public String convertToDatabaseColumn(Phone_Number_Richtype phoneNumber) {
        if (phoneNumber == null) {
            return null;
        }
        return phoneNumber.getValue();
    }

    @Override
    public Phone_Number_Richtype convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return null;
        }
        return Phone_Number_Richtype.of(dbData);
    }
}