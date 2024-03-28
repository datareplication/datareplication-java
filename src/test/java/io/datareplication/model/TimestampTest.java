package io.datareplication.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.format.DateTimeParseException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TimestampTest {

    @Test
    void shouldParseRfc1123DateTime() {
        String dateTime = "Mon, 27 Nov 2023 00:00:00 GMT";

        Timestamp timestamp = Timestamp.fromRfc1123String(dateTime);

        assertThat(timestamp.value()).isEqualTo("2023-11-27T00:00:00.00Z");
    }

    @Test
    void shouldThrowExceptionOnInvalidDateTime() {
        String dateTime = "Wtf, 50 Nov 3999 00:00:00 LOL";

        assertThatThrownBy(() -> Timestamp.fromRfc1123String(dateTime))
            .isInstanceOf(DateTimeParseException.class)
            .hasMessageContaining("Text 'Wtf, 50 Nov 3999 00:00:00 LOL' could not be parsed at index 0");
    }

    @Test
    void shouldReturnTrueIfTimestampIsBeforeOtherTimestamp() {
        Timestamp timestamp1 = Timestamp.of(Instant.parse("2023-11-27T00:00:00.00Z"));
        Timestamp timestamp2 = Timestamp.of(Instant.parse("2023-11-28T00:00:00.00Z"));

        assertThat(timestamp1.isBefore(timestamp2)).isTrue();
    }

    @Test
    void shouldReturnFalseIfTimestampIsNotBeforeOtherTimestamp() {
        Timestamp timestamp1 = Timestamp.of(Instant.parse("2023-11-27T00:00:00.00Z"));
        Timestamp timestamp2 = Timestamp.of(Instant.parse("2023-11-26T00:00:00.00Z"));

        assertThat(timestamp1.isBefore(timestamp2)).isFalse();
    }

    @Test
    void shouldReturnFalseIfTimestampIsEqualOtherTimestamp() {
        Timestamp timestamp1 = Timestamp.of(Instant.parse("2023-11-27T00:00:00.00Z"));
        Timestamp timestamp2 = Timestamp.of(Instant.parse("2023-11-27T00:00:00.00Z"));

        assertThat(timestamp1.isBefore(timestamp2)).isFalse();
    }
}
