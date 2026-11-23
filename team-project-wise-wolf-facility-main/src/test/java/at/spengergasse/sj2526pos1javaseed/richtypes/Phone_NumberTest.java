package at.spengergasse.sj2526pos1javaseed.richtypes;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class Phone_NumberTest {

    @Test
    void valid_phone_number_creates_successfully() {
        Phone_Number_Richtype phone = new Phone_Number_Richtype("+43 660 1234567");
        assertThat(phone.getValue()).isEqualTo("+43 660 1234567");
    }

    @Test
    void null_phone_throws_exception() {
        assertThatThrownBy(() -> new Phone_Number_Richtype(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void blank_phone_throws_exception() {
        assertThatThrownBy(() -> new Phone_Number_Richtype("   "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void phone_with_letters_throws_exception() {
        assertThatThrownBy(() -> new Phone_Number_Richtype("123abc456"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void too_short_phone_throws_exception() {
        assertThatThrownBy(() -> new Phone_Number_Richtype("12"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void of_factory_method_works() {
        Phone_Number_Richtype phone = Phone_Number_Richtype.of("123456789");
        assertThat(phone.getValue()).isEqualTo("123456789");
    }

    @Test
    void two_equal_phones_are_equal() {
        Phone_Number_Richtype p1 = new Phone_Number_Richtype("123456789");
        Phone_Number_Richtype p2 = new Phone_Number_Richtype("123456789");
        assertThat(p1).isEqualTo(p2);
        assertThat(p1.hashCode()).isEqualTo(p2.hashCode());
    }

    @Test
    void leading_and_trailing_spaces_are_trimmed() {
        Phone_Number_Richtype phone = new Phone_Number_Richtype("  123456789  ");
        assertThat(phone.getValue()).isEqualTo("123456789");
    }
}
