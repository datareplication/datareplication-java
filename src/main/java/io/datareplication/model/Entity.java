package io.datareplication.model;

import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.Value;

import java.util.Optional;

@Value
@AllArgsConstructor
public class Entity<Header extends ToHttpHeaders> implements ToHttpHeaders {
    @NonNull Header header;
    @NonNull Body body;
    @NonNull Optional<Object> userData;

    public Entity(@NonNull Header header, @NonNull Body body) {
        this(header, body, Optional.empty());
    }

    @Override
    public @NonNull HttpHeaders toHttpHeaders() {
        return header.toHttpHeaders().update(body.toHttpHeaders());
    }
}
