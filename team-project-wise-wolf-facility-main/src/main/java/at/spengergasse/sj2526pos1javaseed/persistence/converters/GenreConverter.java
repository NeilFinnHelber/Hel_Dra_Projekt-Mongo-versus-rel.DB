package at.spengergasse.sj2526pos1javaseed.persistence.converters;

import at.spengergasse.sj2526pos1javaseed.Domain.Genre;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class GenreConverter implements AttributeConverter<Genre, String> {


    @Override
    public String convertToDatabaseColumn(Genre attribute) {
        return switch (attribute) {
            case null -> null;
            case Fantasy -> "F";
            case Crime -> "CR";
            case Drama -> "DR";
            case SciFi -> "S";
            case Action -> "A";
            case Comedy -> "CO";
            case Horror -> "HO";
            case Mystery -> "M";
            case Romance -> "R";
            case Western -> "W";
            case Thriller -> "T";
            case Adventure -> "AD";
            case Animation -> "AN";
            case Historical -> "HI";
            case Documentary -> "DO";
        };
    }

    @Override
    public Genre convertToEntityAttribute(String dbData) {
        return switch (dbData) {
            case null -> null;
            case "F" -> Genre.Fantasy;
            case "CR" -> Genre.Crime;
            case "DR" -> Genre.Drama;
            case "S" -> Genre.SciFi;
            case "A" -> Genre.Action;
            case "CO" -> Genre.Comedy;
            case "HO" -> Genre.Horror;
            case "M" -> Genre.Mystery;
            case "R" -> Genre.Romance;
            case "W" -> Genre.Western;
            case "T" -> Genre.Thriller;
            case "AD" -> Genre.Adventure;
            case "AN" -> Genre.Animation;
            case "HI" -> Genre.Historical;
            case "DO" -> Genre.Documentary;
            default -> throw DataQualityException.forUnknownString(dbData, Genre.class);
        };
    }
}