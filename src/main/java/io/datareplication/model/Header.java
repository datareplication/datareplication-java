package io.datareplication.model;

import lombok.Value;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

@Value
public class Header {
    String name;
    List<String> values;

    private Header(String name, List<String> values) {
        this.name = name;
        this.values = Collections.unmodifiableList(values);
    }

    public static Header of(String name, String value) {
        return new Header(name, Collections.singletonList(value));
    }

    public static Header of(String name, List<String> values) {
        return new Header(name, values);
    }

    public static final String LAST_MODIFIED = "Last-Modified";
    public static final String CONTENT_TYPE = "Content-Type";
    public static final String CONTENT_LENGTH = "Content-Length";
    public static final String CONTENT_ID = "Content-ID";
    public static final String OPERATION_TYPE = "Operation-Type";
    public static final String LINK = "Link";

    public static Header contentType(ContentType contentType) {
        return Header.of(CONTENT_TYPE, contentType.value());
    }

    public static Header contentLength(int contentLength) {
        return Header.of(CONTENT_LENGTH, Integer.toString(contentLength));
    }

    public static Header lastModified(Instant lastModified) {
        throw new RuntimeException("not implemented");
    }
}
