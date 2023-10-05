package io.datareplication.model.snapshot;

import io.datareplication.model.HttpHeaders;
import io.datareplication.model.ToHttpHeaders;
import lombok.NonNull;
import lombok.Value;

@Value
public class SnapshotEntityHeader implements ToHttpHeaders {
    @NonNull HttpHeaders extraHeaders;

    @Override
    public @NonNull HttpHeaders toHttpHeaders() {
        return extraHeaders;
    }
}
