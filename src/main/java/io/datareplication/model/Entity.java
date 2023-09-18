package io.datareplication.model;

import lombok.Value;

@Value(staticConstructor = "of")
public class Entity<Header extends ToHttpHeaders> implements ToHttpHeaders {
    Header header;
    Body body;

    @Override
    public HttpHeaders toHttpHeaders() {
        return header.toHttpHeaders().update(body);
    }
}
