package io.datareplication.model.snapshot;

import io.datareplication.model.HttpHeader;
import io.datareplication.model.HttpHeaders;
import io.datareplication.model.Timestamp;
import io.datareplication.model.ToHttpHeaders;
import lombok.Value;

@Value
public class EntityHeader implements ToHttpHeaders {
    Timestamp lastModified;
    HttpHeaders extraHeaders;

    @Override
    public HttpHeaders toHttpHeaders() {
        return extraHeaders
                .update(HttpHeader.lastModified(lastModified));
    }
}
