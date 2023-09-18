package io.datareplication.model;

import lombok.EqualsAndHashCode;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@EqualsAndHashCode
public class HttpHeaders implements Iterable<Header>, ToHttpHeaders {
    private final Map<String, Header> headers;

    private HttpHeaders(Map<String, Header> headers) {
        this.headers = Collections.unmodifiableMap(headers);
    }

    @Override
    public Iterator<Header> iterator() {
        return headers.values().iterator();
    }

    public HttpHeaders remove(String headerName) {
        HashMap<String, Header> headerMap = new HashMap<>(headers);
        headerMap.remove(headerName);
        return new HttpHeaders(headerMap);
    }

    public HttpHeaders update(Header... headers) {
        return update(Arrays.asList(headers));
    }

    public HttpHeaders update(Iterable<Header> headers) {
        return update(new HashMap<>(this.headers), headers);
    }

    private static HttpHeaders update(HashMap<String, Header> headerMap, Iterable<Header> headers) {
        for (Header header : headers) {
            headerMap.put(header.name(), header);
        }
        return new HttpHeaders(headerMap);
    }

    @Override
    public HttpHeaders toHttpHeaders() {
        return this;
    }

    public static HttpHeaders of(Iterable<Header> headers) {
        return update(new HashMap<>(), headers);
    }

    public static final HttpHeaders EMPTY = new HttpHeaders(Collections.emptyMap());
}
