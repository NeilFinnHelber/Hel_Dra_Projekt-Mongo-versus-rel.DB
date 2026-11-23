package at.spengergasse.sj2526pos1javaseed.richtypes;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotBlank;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.regex.Pattern;

//simple regex for https://en.wikipedia.org/wiki/Email_address


@Embeddable
@Getter
@ToString
@EqualsAndHashCode
public class Email {

    @NotBlank
    @Column(name = "email")
    private final String value;

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "(?i)^(?:[a-z0-9!#$%&'*+/=?^_`{|}~-]+" +
                    "(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*|" +
                    "\"(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f !#-\\[\\]-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])*\")@" +
                    "(?:" +
                    "(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?(?:\\.[a-z0-9](?:[a-z0-9-]*[a-z0-9])?)*)|" +
                    "\\[(?:IPv6:[0-9a-f:.]+|\\d{1,3}(?:\\.\\d{1,3}){3})\\]" +
                    ")$"
    );


    protected Email() {
        this.value = null;
    }

    public Email(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Email cannot be null");
        }

        // Split local-part and domain
        int atIndex = value.indexOf('@');
        if (atIndex < 1) {
            throw new IllegalArgumentException("Invalid email address: " + value);
        }

        String localPart = value.substring(0, atIndex);
        String domainPart = value.substring(atIndex + 1);

        if (localPart.length() > 64) {
            throw new IllegalArgumentException("Local part too long: " + localPart);
        }
        if (domainPart.length() > 255) {
            throw new IllegalArgumentException("Domain part too long: " + domainPart);
        }

        if (!EMAIL_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("Invalid email address: " + value);
        }

        this.value = value;
    }

    public static Email of(String value) {
        return new Email(value);
    }
}
