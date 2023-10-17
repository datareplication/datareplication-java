package io.datareplication.model;

import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.ToString;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * A collection of multiple {@link HttpHeader} objects. There's only one set of values per name so repeated headers
 * must be merged into a single {@link HttpHeader} with multiple values. The iteration order of headers is not
 * guaranteed and the order is not preserved.
 */
@EqualsAndHashCode
@ToString
public final class HttpHeaders implements Iterable<@NonNull HttpHeader>, ToHttpHeaders {
    @NonNull
    private final Map<@NonNull String, @NonNull HttpHeader> headers;

    /**
     * An empty HttpHeaders instance.
     */
    @NonNull
    public static final HttpHeaders EMPTY = new HttpHeaders(Collections.emptyMap());

    private HttpHeaders(Map<@NonNull String, @NonNull HttpHeader> headers) {
        this.headers = headers;
    }

    /**
     * Iterate over the headers in this object. Iteration order is arbitrary and not guaranteed to be consistent.
     *
     * @return an iterator over all {@link HttpHeader} values in this collection
     */
    @Override
    public @NonNull Iterator<@NonNull HttpHeader> iterator() {
        return headers.values().iterator();
    }

    /**
     * Return a {@link Stream} over the headers in this object.
     *
     * @return a {@link Stream} over all {@link HttpHeader} values in this collection
     */
    public @NonNull Stream<@NonNull HttpHeader> stream() {
        return StreamSupport.stream(spliterator(), false);
    }

    /**
     * Return a new HttpHeaders with the given headers updated. Any header names that are already present will
     * be replaced, i.e. values are <em>not</em> merged.
     *
     * @param headers the headers to update
     * @return a new HttpHeaders with the given updates
     */
    public @NonNull HttpHeaders update(@NonNull HttpHeader... headers) {
        return update(Arrays.stream(headers).iterator());
    }

    /**
     * Return a new HttpHeaders with the given headers updated. Any header names that are already present will
     * be replaced, i.e. values are <em>not</em> merged.
     *
     * @param headers the headers to update
     * @return a new HttpHeaders with the given updates
     */
    public @NonNull HttpHeaders update(@NonNull Iterable<@NonNull HttpHeader> headers) {
        return update(new HashMap<>(this.headers), headers.iterator());
    }

    /**
     * Return a new HttpHeaders with the given headers updated. Any header names that are already present will
     * be replaced, i.e. values are <em>not</em> merged.
     *
     * @param headers the headers to update
     * @return a new HttpHeaders with the given updates
     */
    public @NonNull HttpHeaders update(@NonNull Iterator<@NonNull HttpHeader> headers) {
        return update(new HashMap<>(this.headers), headers);
    }

    private static HttpHeaders update(Map<String, HttpHeader> headerMap, Iterator<HttpHeader> headers) {
        while (headers.hasNext()) {
            final HttpHeader header = headers.next();
            headerMap.merge(header.name(), header, (present, added) -> present.append(added.values()));
        }
        return new HttpHeaders(headerMap);
    }

    /**
     * @return Check if this HttpHeaders is empty.
     */
    public boolean isEmpty() {
        return headers.isEmpty();
    }

    @Override
    public @NonNull HttpHeaders toHttpHeaders() {
        return this;
    }

    /**
     * Create a new HttpHeaders from the given headers. Only the last value for any given header name will be kept.
     *
     * @param headers the headers to include
     * @return a new HttpHeaders from the given headers.
     */
    public static @NonNull HttpHeaders of(@NonNull HttpHeader... headers) {
        return update(new HashMap<>(), Arrays.stream(headers).iterator());
    }

    /**
     * Create a new HttpHeaders from the given headers. Only the last value for any given header name will be kept.
     *
     * @param headers the headers to include
     * @return a new HttpHeaders from the given headers.
     */
    public static @NonNull HttpHeaders of(@NonNull Iterable<@NonNull HttpHeader> headers) {
        return update(new HashMap<>(), headers.iterator());
    }

    /**
     * Create a new HttpHeaders from the given headers. Only the last value for any given header name will be kept.
     *
     * @param headers the headers to include
     * @return the HttpHeaders
     */
    public static @NonNull HttpHeaders of(@NonNull Iterator<@NonNull HttpHeader> headers) {
        return update(new HashMap<>(), headers);
    }
}
