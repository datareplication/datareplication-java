package io.datareplication.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class HttpHeadersTest {
    private static final HttpHeader HEADER_1_1 = HttpHeader.of("X-1", "value1");
    private static final HttpHeader HEADER_1_2 = HttpHeader.of("X-1", "value2");
    private static final HttpHeader HEADER_1_3 = HttpHeader.of("X-1", "value3");
    private static final HttpHeader HEADER_2 = HttpHeader.of("X-2", "value4");
    private static final HttpHeader HEADER_3_1 = HttpHeader.of("X-3", "value5");
    private static final HttpHeader HEADER_3_2 = HttpHeader.of("X-3", "value6");

    @Test
    void emptyHeadersShouldBeEmpty() {
        assertThat(HttpHeaders.EMPTY.isEmpty()).isTrue();
        assertThat(HttpHeaders.of().isEmpty()).isTrue();
        assertThat(HttpHeaders.of()).isEqualTo(HttpHeaders.EMPTY);
    }

    @Test
    void shouldIterateHeaders() {
        final HttpHeaders headers = HttpHeaders.of(HEADER_1_1, HEADER_2, HEADER_3_1);

        assertThat(headers.isEmpty()).isFalse();
        assertThat(headers).containsExactlyInAnyOrder(HEADER_1_1, HEADER_2, HEADER_3_1);
    }

    @Test
    void shouldCreateHeadersFromIterable() {
        final HttpHeaders headers = HttpHeaders.of(List.of(HEADER_1_1, HEADER_2, HEADER_3_1));

        assertThat(headers.isEmpty()).isFalse();
        assertThat(headers).containsExactlyInAnyOrder(HEADER_1_1, HEADER_2, HEADER_3_1);
    }

    @Test
    void shouldRemoveDuplicateHeaders() {
        final HttpHeaders headers = HttpHeaders.of(
            HEADER_1_1,
            HEADER_1_2,
            HEADER_2,
            HEADER_3_2,
            HEADER_1_3,
            HEADER_3_1
        );

        assertThat(headers).containsExactlyInAnyOrder(
            HEADER_1_3,
            HEADER_2,
            HEADER_3_1
        );
    }

    @Test
    void shouldUpdate() {
        final HttpHeaders headers = HttpHeaders.of(
            HEADER_1_1,
            HEADER_2,
            HEADER_3_1
        );

        final HttpHeaders updated = headers.update(HEADER_1_2, HEADER_3_2);

        assertThat(headers).containsExactlyInAnyOrder(
            HEADER_1_1,
            HEADER_2,
            HEADER_3_1
        );
        assertThat(updated).containsExactlyInAnyOrder(
            HEADER_1_2,
            HEADER_2,
            HEADER_3_2
        );
    }

    @Test
    void shouldUpdateFromIterable() {
        final HttpHeaders headers = HttpHeaders.of(
            HEADER_1_1,
            HEADER_2,
            HEADER_3_1
        );

        final HttpHeaders updated = headers.update(List.of(HEADER_1_3));

        assertThat(headers).containsExactlyInAnyOrder(
            HEADER_1_1,
            HEADER_2,
            HEADER_3_1
        );
        assertThat(updated).containsExactlyInAnyOrder(
            HEADER_1_3,
            HEADER_2,
            HEADER_3_1
        );
    }
}
