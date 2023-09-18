package io.datareplication.model;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

public interface Body extends ToHttpHeaders {
    InputStream newInputStream();

    int contentLength();

    // TODO: here?
    ContentType contentType();

    @Override
    default HttpHeaders toHttpHeaders() {
        ArrayList<Header> headers = new ArrayList<>();
        headers.add(Header.contentType(contentType()));
        int contentLength = contentLength();
        if (contentLength > 0) {
            headers.add(Header.contentLength(contentLength));
        }
        return HttpHeaders.of(headers);
    }

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
