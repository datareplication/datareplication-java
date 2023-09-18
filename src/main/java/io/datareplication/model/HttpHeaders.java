package io.datareplication.model;

import lombok.Value;

import java.util.Collections;
import java.util.List;

@Value(staticConstructor = "of")
public class HttpHeaders implements ToHttpHeaders {
    List<Header> headers;

    private HttpHeaders(List<Header> headers) {
        this.headers = Collections.unmodifiableList(headers);
    }

    /*public HttpHeaders update(Header... headers) {

    }

    public HttpHeaders add(Header... headers) {

    }

    public HttpHeaders update()*/

    @Override
    public HttpHeaders toHttpHeaders() {
        return this;
    }

    public static final HttpHeaders EMPTY = HttpHeaders.of(Collections.emptyList());
}
