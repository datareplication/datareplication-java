package io.datareplication.model.snapshot;

import io.datareplication.model.HttpHeaders;
import io.datareplication.model.ToHttpHeaders;
import lombok.NonNull;
import lombok.Value;

/**
 * This class represents the headers of a snapshot page.
 */
@Value
public class SnapshotPageHeader implements ToHttpHeaders {
    /**
     * Additional unstructured headers.
     */
    @NonNull HttpHeaders extraHeaders;

    @Override
    public @NonNull HttpHeaders toHttpHeaders() {
        return extraHeaders;
    }
}
