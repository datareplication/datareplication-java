package io.datareplication.model.snapshot;

import io.datareplication.model.Header;
import io.datareplication.model.HttpHeaders;
import io.datareplication.model.ToHttpHeaders;
import lombok.Value;

import java.time.Instant;

@Value
public class EntityHeader implements ToHttpHeaders {
    Instant lastModified;
    HttpHeaders extraHeaders;

    @Override
    public HttpHeaders toHttpHeaders() {
        return extraHeaders
                .update(Header.lastModified(lastModified));
    }
}
