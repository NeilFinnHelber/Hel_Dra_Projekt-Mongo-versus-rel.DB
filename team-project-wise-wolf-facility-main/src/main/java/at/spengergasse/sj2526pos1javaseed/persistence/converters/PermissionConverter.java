package at.spengergasse.sj2526pos1javaseed.persistence.converters;

import at.spengergasse.sj2526pos1javaseed.Domain.Permission;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class PermissionConverter implements AttributeConverter<Permission, Character> {

        @Override
        public Character convertToDatabaseColumn(Permission attribute) {
            //switch expression
            return switch (attribute) {
                case null -> null;
                case Admin -> 'A';
                case Customer -> 'C';
                case Employee -> 'E';
            };
        }

        @Override
        public Permission convertToEntityAttribute(Character dbData) {
            return switch (dbData) {
                case null -> null;
                case 'A' -> Permission.Admin;
                case 'C' -> Permission.Customer;
                case 'E' -> Permission.Employee;
                default -> throw DataQualityException.forUnknownCharacter(dbData, Permission.class);
            };
        }
}
