package io.datareplication.model;

import lombok.NonNull;

/**
 * The <code>ToHttpHeaders</code> interface allows extracting HTTP for an object.
 */
public interface ToHttpHeaders {
    // TODO error handling?
    /**
     * Return HTTP headers for this object.
     *
     * @return HTTP headers associated with this object
     */
    @NonNull HttpHeaders toHttpHeaders();
}
