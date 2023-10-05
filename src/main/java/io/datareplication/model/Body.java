package io.datareplication.model;

import lombok.NonNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public interface Body extends ToHttpHeaders {
    @NonNull InputStream newInputStream();

    long contentLength();

    // TODO: here?
    @NonNull ContentType contentType();

    @Override
    default @NonNull HttpHeaders toHttpHeaders() {
        ArrayList<HttpHeader> headers = new ArrayList<>();
        headers.add(HttpHeader.contentType(contentType()));
        long contentLength = contentLength();
        if (contentLength > 0) {
            headers.add(HttpHeader.contentLength(contentLength));
        }
        return HttpHeaders.of(headers);
    }

    default @NonNull String toUtf8() throws IOException {
        return new String(newInputStream().readAllBytes(), StandardCharsets.UTF_8);
    }

    default @NonNull byte[] toBytes() throws IOException {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream((int) contentLength())) {
            try (InputStream input = newInputStream()) {
                input.transferTo(output);
            }
            return output.toByteArray();
        }
    }

    static @NonNull Body fromUtf8(@NonNull String utf8) {
        return fromUtf8(utf8, ContentType.of("text/plain; charset=utf-8"));
    }

    static @NonNull Body fromUtf8(@NonNull String utf8, @NonNull ContentType contentType) {
        return new Body() {
            @Override
            public @NonNull InputStream newInputStream() {
                return new ByteArrayInputStream(utf8.getBytes());
            }

            @Override
            public long contentLength() {
                return utf8.getBytes().length;
            }

            @Override
            public @NonNull ContentType contentType() {
                return contentType;
            }
        };
    }

    static @NonNull Body fromBytes(@NonNull byte[] bytes) {
        return fromBytes(bytes, ContentType.of("application/octet-stream"));
    }

    static @NonNull Body fromBytes(@NonNull byte[] bytes, @NonNull ContentType contentType) {
        return new Body() {
            @Override
            public @NonNull InputStream newInputStream() {
                return new ByteArrayInputStream(bytes);
            }

            @Override
            public long contentLength() {
                return bytes.length;
            }

            @Override
            public @NonNull ContentType contentType() {
                return contentType;
            }
        };
    }
}
