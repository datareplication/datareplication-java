package io.datareplication.model;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public interface Body {
    InputStream newInputStream();

    int contentLength();

    // TODO: here?
    ContentType contentType();

    default String toUtf8() {
        throw new RuntimeException("not implemented");
    }

    default byte[] toBytes() throws IOException {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream(contentLength())) {
            try (InputStream input = newInputStream()) {
                input.transferTo(output);
            }
            return output.toByteArray();
        }
    }

    static Body fromUtf8(String utf8) {
        return fromUtf8(utf8, ContentType.of("text/plain; charset=utf-8"));
    }

    static Body fromUtf8(String utf8, ContentType contentType) {
        throw new RuntimeException("not implemented");
    }

    static Body fromBytes(byte[] bytes) {
        return fromBytes(bytes, ContentType.of("application/octet-stream"));
    }

    static Body fromBytes(byte[] bytes, ContentType contentType) {
        return new Body() {
            @Override
            public InputStream newInputStream() {
                return new ByteArrayInputStream(bytes);
            }

            @Override
            public int contentLength() {
                return bytes.length;
            }

            @Override
            public ContentType contentType() {
                return contentType;
            }
        };
    }
}
