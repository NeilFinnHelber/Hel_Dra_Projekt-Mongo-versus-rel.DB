package at.spengergasse.sj2526pos1javaseed.persistence.converters;

public class DataQualityException extends RuntimeException{
    private DataQualityException(String message)
    {
        super(message);
    }

    public static DataQualityException forUnknownCharacter(Character character, Class<?> clazz)
    {
        var msg = "Unknown character '%s' found for class '%s'" .formatted(character, clazz.getSimpleName());
        return new DataQualityException(msg);
    }

    public static DataQualityException forUnknownString(String string, Class<?> clazz)
    {
        var msg = "Unknown String '%s' found for class '%s'" .formatted(string, clazz.getSimpleName());
        return new DataQualityException(msg);
    }
}
