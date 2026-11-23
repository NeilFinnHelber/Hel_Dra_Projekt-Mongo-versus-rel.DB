package at.spengergasse.sj2526pos1javaseed.persistence.converters;

import at.spengergasse.sj2526pos1javaseed.Domain.Language;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class LanguageConverter implements AttributeConverter<Language, String> {

        @Override
        public String convertToDatabaseColumn(Language attribute) {
            //switch expression
            return switch (attribute) {
                case null -> null;
                case French -> "F";
                case English -> "E";
                case Spanish -> "S";
                case German -> "G";
                case Japanese -> "J";
            };
        }

        @Override
        public Language convertToEntityAttribute(String dbData) {
            return switch (dbData) {
                case null -> null;
                case "F" -> Language.French;
                case "E" -> Language.English;
                case "S" -> Language.Spanish;
                case "G" -> Language.German;
                case "J" -> Language.Japanese;
                default -> throw DataQualityException.forUnknownString(dbData, Language.class);
            };
        }

}
