package io.datareplication.model;

import lombok.NonNull;
import lombok.Value;

import java.time.Instant;

/**
 * A timestamp mostly for HTTP <code>Last-Modified</code> headers. Note that while {@link Instant} has nanosecond
 * resolution, HTTP header datetime formats only have 1-second resolution.
 */
@Value(staticConstructor = "of")
public class Timestamp {
    @NonNull Instant value;
}
