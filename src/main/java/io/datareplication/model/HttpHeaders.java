package io.datareplication.model;

import lombok.EqualsAndHashCode;
import lombok.NonNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@EqualsAndHashCode
public class HttpHeaders implements Iterable<@NonNull HttpHeader>, ToHttpHeaders {
    @NonNull private final Map<@NonNull String, @NonNull HttpHeader> headers;

    private HttpHeaders(Map<@NonNull String, @NonNull HttpHeader> headers) {
        this.headers = Collections.unmodifiableMap(headers);
    }

    @Override
    public @NonNull Iterator<@NonNull HttpHeader> iterator() {
        return headers.values().iterator();
    }

    public @NonNull HttpHeaders remove(@NonNull String headerName) {
        HashMap<String, HttpHeader> headerMap = new HashMap<>(headers);
        headerMap.remove(headerName);
        return new HttpHeaders(headerMap);
    }

    public @NonNull HttpHeaders update(@NonNull HttpHeader... headers) {
        return update(Arrays.asList(headers));
    }

    public @NonNull HttpHeaders update(@NonNull Iterable<@NonNull HttpHeader> headers) {
        return update(new HashMap<>(this.headers), headers);
    }

    private static HttpHeaders update(HashMap<String, HttpHeader> headerMap, Iterable<HttpHeader> headers) {
        for (HttpHeader header : headers) {
            headerMap.put(header.name(), header);
        }
        return new HttpHeaders(headerMap);
    }

    @Override
    public @NonNull HttpHeaders toHttpHeaders() {
        return this;
    }

    public static @NonNull HttpHeaders of(@NonNull Iterable<@NonNull HttpHeader> headers) {
        return update(new HashMap<>(), headers);
    }

    @NonNull public static final HttpHeaders EMPTY = new HttpHeaders(Collections.emptyMap());
}
