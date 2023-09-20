package io.datareplication.model;

import lombok.Value;

import java.util.Collections;
import java.util.List;

@Value
public class HttpHeader {
    String name;
    List<String> values;

    private HttpHeader(String name, List<String> values) {
        this.name = name;
        this.values = Collections.unmodifiableList(values);
    }

    public static HttpHeader of(String name, String value) {
        return new HttpHeader(name, Collections.singletonList(value));
    }

    public static HttpHeader of(String name, List<String> values) {
        return new HttpHeader(name, values);
    }

    public static final String LAST_MODIFIED = "Last-Modified";
    public static final String CONTENT_TYPE = "Content-Type";
    public static final String CONTENT_LENGTH = "Content-Length";
    public static final String CONTENT_ID = "Content-ID";
    public static final String OPERATION_TYPE = "Operation-Type";
    public static final String LINK = "Link";

    public static HttpHeader contentType(ContentType contentType) {
        return HttpHeader.of(CONTENT_TYPE, contentType.value());
    }

    public static HttpHeader contentLength(int contentLength) {
        return HttpHeader.of(CONTENT_LENGTH, Integer.toString(contentLength));
    }

    public static HttpHeader lastModified(Timestamp lastModified) {
        throw new RuntimeException("not implemented");
    }
}
