package io.datareplication.model;

import lombok.NonNull;

public interface ToHttpHeaders {
    // TODO error handling?
    @NonNull HttpHeaders toHttpHeaders();
}
