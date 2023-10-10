package io.datareplication.model.snapshot;

import io.datareplication.model.HttpHeader;
import io.datareplication.model.HttpHeaders;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SnapshotPageHeaderTest {
    @Test
    void shouldReturnHttpHeaders() {
        final HttpHeaders httpHeaders = HttpHeaders.of(HttpHeader.of("h1", "v1"),
                                                       HttpHeader.of("h2", "v2"));
        final SnapshotPageHeader pageHeader = new SnapshotPageHeader(httpHeaders);

        assertThat(pageHeader.toHttpHeaders()).isEqualTo(httpHeaders);
    }
}
