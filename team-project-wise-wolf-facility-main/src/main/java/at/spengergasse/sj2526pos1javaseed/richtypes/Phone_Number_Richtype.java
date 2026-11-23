package at.spengergasse.sj2526pos1javaseed.richtypes;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotBlank;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.regex.Pattern;

@Embeddable
@Getter
@ToString
@EqualsAndHashCode
public class Phone_Number_Richtype {

    @NotBlank
    @Column(name = "phone_number")
    private final String value;

    private static final Pattern PHONE_PATTERN = Pattern.compile("^[^A-Za-z]*$");

    protected Phone_Number_Richtype() {
        this.value = null;
    }

    public Phone_Number_Richtype(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Phone number cannot be null");
        }

        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("Phone number cannot be blank");
        }

        if (!PHONE_PATTERN.matcher(trimmed).matches()) {
            throw new IllegalArgumentException("Phone number must not contain letters: " + value);
        }

        if (trimmed.length() < 4) {
            throw new IllegalArgumentException("Phone number too short: " + trimmed);
        }

        this.value = trimmed;
    }

    public static Phone_Number_Richtype of(String value) {
        return new Phone_Number_Richtype(value);
    }
}

