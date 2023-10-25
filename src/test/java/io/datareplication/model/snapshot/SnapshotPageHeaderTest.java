package io.datareplication.model.snapshot;

import io.datareplication.model.HttpHeader;
import io.datareplication.model.HttpHeaders;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SnapshotPageHeaderTest {
    @Test
    void shouldDefaultToEmptyExtraHeaders() {
        final SnapshotPageHeader header = new SnapshotPageHeader();

        assertThat(header.extraHeaders()).isEqualTo(HttpHeaders.EMPTY);
    }

    @Test
    void shouldReturnHttpHeaders() {
        final HttpHeaders httpHeaders = HttpHeaders.of(HttpHeader.of("h1", "v1"),
                                                       HttpHeader.of("h2", "v2"));
        final SnapshotPageHeader header = new SnapshotPageHeader(httpHeaders);

        assertThat(header.toHttpHeaders()).isEqualTo(httpHeaders);
    }
}
