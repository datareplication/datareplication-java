package io.datareplication.model;

import lombok.NonNull;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class HttpHeadersTest {
    private static final HttpHeader HEADER_1 = HttpHeader.of("X-1", "value1");
    private static final HttpHeader HEADER_2 = HttpHeader.of("X-2", "value4");
    private static final HttpHeader HEADER_3 = HttpHeader.of("X-3", "value5");

    @Test
    void emptyHeadersShouldBeEmpty() {
        assertThat(HttpHeaders.EMPTY.isEmpty()).isTrue();
        assertThat(HttpHeaders.of().isEmpty()).isTrue();
        assertThat(HttpHeaders.of()).isEqualTo(HttpHeaders.EMPTY);
    }

    @Test
    void shouldIterateHeaders() {
        final HttpHeaders headers = HttpHeaders.of(HEADER_1, HEADER_2, HEADER_3);

        assertThat(headers.isEmpty()).isFalse();
        assertThat(headers).containsExactlyInAnyOrder(HEADER_1, HEADER_2, HEADER_3);
    }

    @Test
    void shouldStreamHeaders() {
        final HttpHeaders headers = HttpHeaders.of(HEADER_1, HEADER_2, HEADER_3);

        assertThat(headers.isEmpty()).isFalse();
        assertThat(headers.stream()).containsExactlyInAnyOrder(HEADER_1, HEADER_2, HEADER_3);
    }

    @Test
    void of_shouldCreateHeadersFromIterable() {
        final HttpHeaders headers = HttpHeaders.of(List.of(HEADER_1, HEADER_2, HEADER_3));

        assertThat(headers.isEmpty()).isFalse();
        assertThat(headers).containsExactlyInAnyOrder(HEADER_1, HEADER_2, HEADER_3);
    }

    @Test
    void of_shouldCreateHeadersFromIterator() {
        final HttpHeaders headers = HttpHeaders.of(List.of(HEADER_1, HEADER_2, HEADER_3).iterator());

        assertThat(headers.isEmpty()).isFalse();
        assertThat(headers).containsExactlyInAnyOrder(HEADER_1, HEADER_2, HEADER_3);
    }

    @Test
    void of_shouldMergeHeadersCaseInsensitive() {
        final HttpHeaders headers = HttpHeaders.of(
            HttpHeader.of("abc-1", "value1"),
            HttpHeader.of("ABC-2", "value2"),
            HttpHeader.of("ABC-1", "value1"),
            HttpHeader.of("abC-2", List.of("value3", "value4")),
            HttpHeader.of("Abc-2", "value5"),
            HttpHeader.of("ABC-1", List.of("value6", "value7")),
            HttpHeader.of("aBC-1", "value8")
        );

        assertThat(headers.isEmpty()).isFalse();
        assertThat(headers).containsExactlyInAnyOrder(
            HttpHeader.of("abc-1", List.of("value1", "value1", "value6", "value7", "value8")),
            HttpHeader.of("ABC-2", List.of("value2", "value3", "value4", "value5"))
        );
    }

    @Test
    void update_shouldMerge() {
        final HttpHeaders headers = HttpHeaders.of(
            HttpHeader.of("abc-1", "value1"),
            HttpHeader.of("ABC-2", "value2")
        );

        final HttpHeaders updated = headers.update(
            HttpHeader.of("ABC-1", "value3"),
            HttpHeader.of("abc-3", "value4")
        );

        assertThat(updated).containsExactlyInAnyOrder(
            HttpHeader.of("abc-1", List.of("value1", "value3")),
            HttpHeader.of("ABC-2", "value2"),
            HttpHeader.of("abc-3", "value4")
        );
    }

    @Test
    void update_shouldMergeFromIterable() {
        final HttpHeaders headers = HttpHeaders.of(
            HttpHeader.of("abc-1", "value1"),
            HttpHeader.of("ABC-2", "value2")
        );

        final HttpHeaders updated = headers.update(List.of(
            HttpHeader.of("ABC-1", "value3"),
            HttpHeader.of("abc-3", "value4")
        ));

        assertThat(updated).containsExactlyInAnyOrder(
            HttpHeader.of("abc-1", List.of("value1", "value3")),
            HttpHeader.of("ABC-2", "value2"),
            HttpHeader.of("abc-3", "value4")
        );
    }

    @Test
    void update_shouldMergeFromIterator() {
        final HttpHeaders headers = HttpHeaders.of(
            HttpHeader.of("abc-1", "value1"),
            HttpHeader.of("ABC-2", "value2")
        );

        final HttpHeaders updated = headers.update(List.of(
            HttpHeader.of("ABC-1", "value3"),
            HttpHeader.of("abc-3", "value4")
        ).iterator());

        assertThat(updated).containsExactlyInAnyOrder(
            HttpHeader.of("abc-1", List.of("value1", "value3")),
            HttpHeader.of("ABC-2", "value2"),
            HttpHeader.of("abc-3", "value4")
        );
    }

    @Test
    void get_shouldReturnHttpHeader() {
        HttpHeader header1 = HttpHeader.of("abc-1", "value1");
        final HttpHeaders headers = HttpHeaders.of(
            header1,
            HttpHeader.of("ABC-2", "value2")
        );

        Optional<@NonNull HttpHeader> optionalHttpHeader = headers.get("abc-1");

        assertThat(optionalHttpHeader).hasValue(header1);
    }

    @Test
    void get_shouldReturnHttpHeaderCaseInsensitive() {
        HttpHeader header1 = HttpHeader.of("abc-1", "value1");
        final HttpHeaders headers = HttpHeaders.of(
            header1,
            HttpHeader.of("ABC-2", "value2")
        );

        Optional<@NonNull HttpHeader> optionalHttpHeader = headers.get("AbC-1");

        assertThat(optionalHttpHeader).hasValue(header1);
    }

    @Test
    void get_shouldReturnNoneOnEmptyHttpHeaders() {
        final HttpHeaders headers = HttpHeaders.EMPTY;

        Optional<@NonNull HttpHeader> optionalHttpHeader = headers.get("abc-1");

        assertThat(optionalHttpHeader).isNotPresent();
    }

    @Test
    void get_shouldReturnNoneOnUnknownKey() {
        final HttpHeaders headers = HttpHeaders.of(
            HttpHeader.of("abc-1", "value1"),
            HttpHeader.of("ABC-2", "value2")
        );

        Optional<@NonNull HttpHeader> optionalHttpHeader = headers.get("...-0");

        assertThat(optionalHttpHeader).isNotPresent();
    }
}
