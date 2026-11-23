package at.spengergasse.sj2526pos1javaseed.richtypes;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class EmailTest {

    @Test
    void valid_email_creates_successfully() {
        Email email = new Email("test@example.com");
        assertThat(email.getValue()).isEqualTo("test@example.com");
    }

    @Test
    void null_email_throws_exception() {
        assertThatThrownBy(() -> new Email(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void missing_at_sign_throws_exception() {
        assertThatThrownBy(() -> new Email("invalidemail.com"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void missing_domain_throws_exception() {
        assertThatThrownBy(() -> new Email("invalid@"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void email_of_factory_method_works() {
        Email email = Email.of("user@domain.org");
        assertThat(email.getValue()).isEqualTo("user@domain.org");
    }

    @Test
    void two_equal_emails_are_equal() {
        Email e1 = new Email("same@example.com");
        Email e2 = new Email("same@example.com");
        assertThat(e1).isEqualTo(e2);
        assertThat(e1.hashCode()).isEqualTo(e2.hashCode());
    }

    @Test
    void two_different_emails_are_not_equal() {
        Email e1 = new Email("a@example.com");
        Email e2 = new Email("b@example.com");
        assertThat(e1).isNotEqualTo(e2);
    }

    @Test
    void email_with_subdomain_is_valid() {
        Email email = new Email("user@mail.example.com");
        assertThat(email.getValue()).isEqualTo("user@mail.example.com");
    }
}
