package io.datareplication.model.feed;

import io.datareplication.model.HttpHeader;
import io.datareplication.model.HttpHeaders;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class FeedEntityHeaderTest {
    @Test
    void shouldDefaultToEmptyExtraHeaders() {
        final FeedEntityHeader header = new FeedEntityHeader(
            Instant.now(),
            OperationType.PUT,
            ContentId.of("1234"));

        assertThat(header.extraHeaders()).isEqualTo(HttpHeaders.EMPTY);
    }

    @Test
    void shouldReturnHttpHeaders() {
        final HttpHeaders extraHeaders = HttpHeaders.of(HttpHeader.of("e1", "v1"));
        final FeedEntityHeader header = new FeedEntityHeader(
            Instant.parse("2023-10-05T09:58:59.000Z"),
            OperationType.PUT,
            ContentId.of("content-id-1234"),
            extraHeaders);

        assertThat(header.toHttpHeaders()).isEqualTo(HttpHeaders.of(
            HttpHeader.of("Last-Modified", "Thu, 05 Oct 2023 09:58:59 GMT"),
            HttpHeader.of("Operation-Type", "http-equiv=PUT"),
            HttpHeader.of("Content-ID", "content-id-1234"),
            HttpHeader.of("e1", "v1")));
    }

    @Test
    void shouldReturnHttpHeadersWithDifferentOperationType() {
        final FeedEntityHeader header = new FeedEntityHeader(
            Instant.parse("2023-10-24T12:14:12.000Z"),
            OperationType.DELETE,
            ContentId.of(""));

        assertThat(header.toHttpHeaders()).isEqualTo(HttpHeaders.of(
            HttpHeader.of("Last-Modified", "Tue, 24 Oct 2023 12:14:12 GMT"),
            HttpHeader.of("Operation-Type", "http-equiv=DELETE"),
            HttpHeader.of("Content-ID", "")));
    }
}
