package at.spengergasse.sj2526pos1javaseed.persistence.converters;

import at.spengergasse.sj2526pos1javaseed.Domain.Country;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class CountryConverter implements AttributeConverter<Country, String> {

    @Override
    public String convertToDatabaseColumn(Country attribute) {
        //switch expression
        return switch (attribute) {
            case null -> null;
            case USA -> "U";
            case Italy -> "I";
            case Japan -> "J";
            case Germany -> "GER";
            case Spain -> "SP";
            case France -> "F";
            case Greece -> "GR";
            case Austria -> "A";
            case Switzerland -> "SW";
        };
    }

    @Override
    public Country convertToEntityAttribute(String dbData) {
        return switch (dbData) {
            case null -> null;
            case "U" -> Country.USA;
            case "I" -> Country.Italy;
            case "J" -> Country.Japan;
            case "GER" -> Country.Germany;
            case "SP" -> Country.Spain;
            case "F" -> Country.France;
            case "GR" -> Country.Greece;
            case "A" -> Country.Austria;
            case "SW" -> Country.Switzerland;
            default -> throw DataQualityException.forUnknownString(dbData, Country.class);
        };
    }
}
