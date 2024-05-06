package io.datareplication.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

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
    void shouldLowerCaseName() {
        final HttpHeader header = HttpHeader.of("Upper-And-Lower-Case", "value");

        assertThat(header.displayName()).isEqualTo("Upper-And-Lower-Case");
        assertThat(header.name()).isEqualTo("upper-and-lower-case");
    }

    @Test
    void shouldExcludeDisplayNameFromHashCodeAndEquals() {
        final HttpHeader header1 = HttpHeader.of("header", "value");
        final HttpHeader header2 = HttpHeader.of("HEADER", "value");

        assertThat(header1).isEqualTo(header2);
        assertThat(header1).hasSameHashCodeAs(header2);
    }

    @Test
    void shouldAppendSingleValue() {
        final HttpHeader header = HttpHeader.of("", List.of("v1", "v2"));

        final HttpHeader result = header.append("v3");

        assertThat(result).isEqualTo(HttpHeader.of("", List.of("v1", "v2", "v3")));
    }

    @Test
    void shouldAppendMultipleValues() {
        final HttpHeader header = HttpHeader.of("", List.of("v1", "v2"));

        final HttpHeader result = header.append(List.of("v2", "v3"));

        assertThat(result).isEqualTo(HttpHeader.of("", List.of("v1", "v2", "v2", "v3")));
    }

    @Test
    void nameEquals_shouldBeTrueIfNamesAreEqual() {
        final HttpHeader header = HttpHeader.of("test-header", "value");

        assertThat(header.nameEquals("test-header")).isTrue();
    }

    @Test
    void nameEquals_shouldBeTrueIfNamesAreEqualIgnoringCase() {
        final HttpHeader header = HttpHeader.of("tesT-headeR", "value");

        assertThat(header.nameEquals("Test-Header")).isTrue();
    }

    @Test
    void nameEquals_shouldBeFalseIfNamesAreNotEqual() {
        final HttpHeader header = HttpHeader.of("test1", "value");

        assertThat(header.nameEquals("test2")).isFalse();
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
        final HttpHeader header = HttpHeader.lastModified(Instant.parse("2023-10-04T08:25:33.666Z"));

        assertThat(header)
            .isEqualTo(HttpHeader.of("Last-Modified", "Wed, 04 Oct 2023 08:25:33 GMT"));
    }
}
