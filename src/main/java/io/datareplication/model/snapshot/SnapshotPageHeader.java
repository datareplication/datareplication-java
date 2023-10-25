package io.datareplication.model.snapshot;

import io.datareplication.model.HttpHeaders;
import io.datareplication.model.ToHttpHeaders;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.Value;

/**
 * This class represents the headers of a snapshot page.
 */
@Value
@AllArgsConstructor
public class SnapshotPageHeader implements ToHttpHeaders {
    /**
     * Additional unstructured headers.
     */
    @NonNull HttpHeaders extraHeaders;

    public SnapshotPageHeader() {
        this(HttpHeaders.EMPTY);
    }

    @Override
    public @NonNull HttpHeaders toHttpHeaders() {
        return extraHeaders;
    }
}
