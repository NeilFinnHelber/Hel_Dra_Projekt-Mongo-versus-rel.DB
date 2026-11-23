package at.spengergasse.sj2526pos1javaseed.richtypes;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class UserNameTest {

    @Test
    void valid_username_creates_successfully() {
        UserName username = new UserName("cooluser99");
        assertThat(username.getValue()).isEqualTo("cooluser99");
    }

    @Test
    void null_username_throws_exception() {
        assertThatThrownBy(() -> new UserName(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void blank_username_throws_exception() {
        assertThatThrownBy(() -> new UserName("   "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void forbidden_term_admin_throws_exception() {
        assertThatThrownBy(() -> new UserName("admin"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void forbidden_term_moderator_throws_exception() {
        assertThatThrownBy(() -> new UserName("moderator"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void ipv4_address_as_username_throws_exception() {
        assertThatThrownBy(() -> new UserName("192.168.0.1"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void timestamp_as_username_throws_exception() {
        assertThatThrownBy(() -> new UserName("2024-01-01"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void of_factory_method_works() {
        UserName username = UserName.of("john_doe");
        assertThat(username.getValue()).isEqualTo("john_doe");
    }

    @Test
    void two_equal_usernames_are_equal() {
        UserName u1 = new UserName("alice");
        UserName u2 = new UserName("alice");
        assertThat(u1).isEqualTo(u2);
        assertThat(u1.hashCode()).isEqualTo(u2.hashCode());
    }

    @Test
    void two_different_usernames_are_not_equal() {
        UserName u1 = new UserName("alice");
        UserName u2 = new UserName("bob");
        assertThat(u1).isNotEqualTo(u2);
    }
}
