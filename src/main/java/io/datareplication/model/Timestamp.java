package io.datareplication.model;

import lombok.NonNull;
import lombok.Value;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * A timestamp mostly for HTTP <code>Last-Modified</code> headers. Note that while {@link Instant} has nanosecond
 * resolution, HTTP header datetime formats only have 1-second resolution.
 */
@Value(staticConstructor = "of")
public class Timestamp {
    @NonNull Instant value;

    /**
     * Return a boolean indicating whether this Timestamp is before the given Timestamp.
     *
     * @param other the other Timestamp to compare to
     * @return <code>true</code> if this Timestamp is before the other Timestamp, <code>false</code> otherwise
     */
    public boolean isBefore(@NonNull Timestamp other) {
        return value.isBefore(other.value);
    }

    /**
     * Return a new Timestamp for the current instant (according to the system clock).
     *
     * @return the Timestamp
     */
    public static @NonNull Timestamp now() {
        return Timestamp.of(Instant.now());
    }

    /**
     * Parses a RFC-1123 date-time, such as 'Tue, 3 Jun 2008 11:05:30 GMT'.
     *
     * @param string the date-time string to parse
     * @return the Timestamp
     * @throws DateTimeParseException when the given string can't be parsed
     */
    public static @NonNull Timestamp fromRfc1123String(@NonNull String string) {
        return Timestamp.of(Instant.from(DateTimeFormatter.RFC_1123_DATE_TIME.parse(string)));
    }
}
