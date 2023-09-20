package io.datareplication.model;

import lombok.AllArgsConstructor;
import lombok.Value;

import java.util.Optional;

@Value
@AllArgsConstructor
public class Entity<Header extends ToHttpHeaders> implements ToHttpHeaders {
    Header header;
    Body body;
    Optional<Object> userData;

    public Entity(Header header, Body body) {
        this(header, body, Optional.empty());
    }

    @Override
    public HttpHeaders toHttpHeaders() {
        return header.toHttpHeaders().update(body.toHttpHeaders());
    }
}
