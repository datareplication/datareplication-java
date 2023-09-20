package io.datareplication.model;

import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@EqualsAndHashCode
public class HttpHeaders implements Iterable<HttpHeader>, ToHttpHeaders {
    private final Map<String, HttpHeader> headers;

    private HttpHeaders(Map<String, HttpHeader> headers) {
        this.headers = Collections.unmodifiableMap(headers);
    }

    @Override
    public Iterator<HttpHeader> iterator() {
        return headers.values().iterator();
    }

    public HttpHeaders remove(String headerName) {
        HashMap<String, HttpHeader> headerMap = new HashMap<>(headers);
        headerMap.remove(headerName);
        return new HttpHeaders(headerMap);
    }

    public HttpHeaders update(HttpHeader... headers) {
        return update(Arrays.asList(headers));
    }

    public HttpHeaders update(Iterable<HttpHeader> headers) {
        return update(new HashMap<>(this.headers), headers);
    }

    private static HttpHeaders update(HashMap<String, HttpHeader> headerMap, Iterable<HttpHeader> headers) {
        for (HttpHeader header : headers) {
            headerMap.put(header.name(), header);
        }
        return new HttpHeaders(headerMap);
    }

    @Override
    public HttpHeaders toHttpHeaders() {
        return this;
    }

    public static HttpHeaders of(Iterable<HttpHeader> headers) {
        return update(new HashMap<>(), headers);
    }

    public static final HttpHeaders EMPTY = new HttpHeaders(Collections.emptyMap());
}
