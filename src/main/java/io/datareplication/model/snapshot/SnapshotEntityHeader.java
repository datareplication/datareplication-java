package io.datareplication.model.snapshot;

import io.datareplication.model.HttpHeaders;
import io.datareplication.model.ToHttpHeaders;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.Value;

/**
 * This class represents the headers of a snapshot entity.
 */
@Value
@AllArgsConstructor
public class SnapshotEntityHeader implements ToHttpHeaders {
    /**
     * Additional unstructured headers.
     */
    @NonNull HttpHeaders extraHeaders;

    public SnapshotEntityHeader() {
        this(HttpHeaders.EMPTY);
    }

    @Override
    public @NonNull HttpHeaders toHttpHeaders() {
        return extraHeaders;
    }
}
