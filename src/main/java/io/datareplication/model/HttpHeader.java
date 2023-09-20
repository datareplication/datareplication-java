package io.datareplication.model;

import lombok.NonNull;
import lombok.Value;

import java.util.Collections;
import java.util.List;

@Value
public class HttpHeader {
    @NonNull String name;
    @NonNull List<@NonNull String> values;

    private HttpHeader(@NonNull String name, @NonNull List<@NonNull String> values) {
        this.name = name;
        this.values = Collections.unmodifiableList(values);
    }

    public static @NonNull HttpHeader of(@NonNull String name, @NonNull String value) {
        return new HttpHeader(name, Collections.singletonList(value));
    }

    public static @NonNull HttpHeader of(@NonNull String name, @NonNull List<@NonNull String> values) {
        return new HttpHeader(name, values);
    }

    @NonNull public static final String LAST_MODIFIED = "Last-Modified";
    @NonNull public static final String CONTENT_TYPE = "Content-Type";
    @NonNull public static final String CONTENT_LENGTH = "Content-Length";
    @NonNull public static final String CONTENT_ID = "Content-ID";
    @NonNull public static final String OPERATION_TYPE = "Operation-Type";
    @NonNull public static final String LINK = "Link";

    public static @NonNull HttpHeader contentType(@NonNull ContentType contentType) {
        return HttpHeader.of(CONTENT_TYPE, contentType.value());
    }

    public static @NonNull HttpHeader contentLength(long contentLength) {
        return HttpHeader.of(CONTENT_LENGTH, Long.toString(contentLength));
    }

    public static @NonNull HttpHeader lastModified(@NonNull Timestamp lastModified) {
        throw new RuntimeException("not implemented");
    }
}
