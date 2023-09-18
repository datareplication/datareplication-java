package io.datareplication.model.snapshot;

import io.datareplication.model.HttpHeaders;
import io.datareplication.model.ToHttpHeaders;
import lombok.Value;

@Value
public class PageHeader implements ToHttpHeaders {
    HttpHeaders extraHeaders;

    @Override
    public HttpHeaders toHttpHeaders() {
        return extraHeaders;
    }
}
