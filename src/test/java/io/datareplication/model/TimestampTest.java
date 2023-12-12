package io.datareplication.model;

import org.junit.jupiter.api.Test;

import java.time.format.DateTimeParseException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TimestampTest {

    @Test
    void shouldParseRfc1123DateTime() {
        String dateTime = "Mon, 27 Nov 2023 00:00:00 GMT";

        Timestamp timestamp = Timestamp.parseRfc1123DateTime(dateTime);

        assertThat(timestamp.value()).isEqualTo("2023-11-27T00:00:00.00Z");
    }

    @Test
    void shouldThrowExceptionOnInvalidDateTime() {
        String dateTime = "Wtf, 50 Nov 3999 00:00:00 LOL";

        assertThatThrownBy(() -> Timestamp.parseRfc1123DateTime(dateTime))
            .isInstanceOf(DateTimeParseException.class)
            .hasMessageContaining("Text 'Wtf, 50 Nov 3999 00:00:00 LOL' could not be parsed at index 0");
    }
}
