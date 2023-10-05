package io.datareplication.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HttpHeaderTest {
    @Test
    void shouldMakeValueListUnmodifiable() {
        final ArrayList<String> original = new ArrayList<>();
        original.add("1");
        original.add("2");
        final ArrayList<String> values = new ArrayList<>(original);

        final HttpHeader header = HttpHeader.of("X-Test", values);

        assertThat(header.values()).containsExactlyElementsOf(original);
        assertThatThrownBy(() -> header.values().add("3"))
            .isInstanceOf(UnsupportedOperationException.class);
        values.add("3");
        assertThat(header.values()).containsExactlyElementsOf(original);
    }

    @Test
    void shouldCreateContentTypeHeader() {
        final HttpHeader header = HttpHeader.contentType(ContentType.of("application/json"));

        assertThat(header)
            .isEqualTo(HttpHeader.of("Content-Type", "application/json"));
    }

    @Test
    void shouldCreateContentLengthHeader() {
        final HttpHeader header = HttpHeader.contentLength(15);

        assertThat(header)
            .isEqualTo(HttpHeader.of("Content-Length", "15"));
    }

    @Test
    void shouldCreateLastModifiedHeader() {
        final HttpHeader header = HttpHeader.lastModified(Timestamp.of(Instant.parse("2023-10-04T08:25:33.666Z")));

        assertThat(header)
            .isEqualTo(HttpHeader.of("Last-Modified", "Wed, 04 Oct 2023 08:25:33 GMT"));
    }
}
