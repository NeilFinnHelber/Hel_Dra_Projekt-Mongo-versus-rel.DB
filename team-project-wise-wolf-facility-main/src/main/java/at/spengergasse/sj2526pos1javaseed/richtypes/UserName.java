package at.spengergasse.sj2526pos1javaseed.richtypes;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotBlank;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import javax.naming.Name;
import java.util.List;
import java.util.regex.Pattern;


//profanities, Non-script usernames, admin implying names
@Embeddable
@Getter
@ToString
@EqualsAndHashCode
public class UserName {

    @NotBlank
    @Column(name = "user_name")
    private final String value;

    private static final List<String> BLACKLIST = List.of(
            "administrator", "admin", "bureaucrat", "steward", "checkuser", "oversight",
            "sysop", "moderator", "wmf", "wikimedia", "bot", "vanished user"
    );

    private static final Pattern INVALID_PATTERN = Pattern.compile(
            "(?:" +
                    // IPv4
                    "\\b\\d{1,3}(?:\\.\\d{1,3}){3}\\b" +
                    "|" +
                    // IPv6
                    "\\b(?:[0-9a-fA-F]{0,4}:){2,7}[0-9a-fA-F]{0,4}\\b" +
                    "|" +
                    // Timestamps
                    "\\b\\d{4}-\\d{2}-\\d{2}\\b" +
                    "|" +
                    // Emoji & non-language symbols
                    "[\\p{So}\\p{Cn}]" +
                    ")"
    );

    protected UserName() {
        this.value = null;
    }

    public UserName(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("UserName may not be blank");
        }

        String lower = value.toLowerCase();

        for (String forbidden : BLACKLIST) {
            if (lower.contains(forbidden)) {
                throw new IllegalArgumentException("Username contains forbidden term: " + forbidden);
            }
        }

        if (INVALID_PATTERN.matcher(value).find()) {
            throw new IllegalArgumentException("Username contains invalid characters or patterns: " + value);
        }

        this.value = value;
    }

    public static UserName of(String value) {
        return new UserName(value);
    }
}

