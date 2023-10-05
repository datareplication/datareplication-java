package io.datareplication.model.feed;

import io.datareplication.model.HttpHeader;
import io.datareplication.model.HttpHeaders;
import io.datareplication.model.Timestamp;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class FeedEntityHeaderTest {
    @Test
    void shouldReturnHttpHeaders() {
        final HttpHeaders extraHeaders = HttpHeaders.of(HttpHeader.of("e1", "v1"));
        final FeedEntityHeader entityHeader = new FeedEntityHeader(
            Timestamp.of(Instant.parse("2023-10-05T09:52:13.123Z")),
            OperationType.PUT,
            ContentId.of("12345"),
            extraHeaders);

        assertThat(entityHeader.toHttpHeaders()).isEqualTo(HttpHeaders.of(
            HttpHeader.of("Last-Modified", "Thu, 05 Oct 2023 09:52:13 GMT"),
            HttpHeader.of("Operation-Type", "http-equiv=PUT"),
            HttpHeader.of("Content-ID", "12345"),
            HttpHeader.of("e1", "v1")));
    }

    @Test
    void shouldReplaceHeadersFromExtraHeadersWithSpecialCasedHeaders() {
        final HttpHeaders extraHeaders = HttpHeaders.of(
            HttpHeader.of("Last-Modified", "haha"),
            HttpHeader.of("Operation-Type", "secret"),
            HttpHeader.of("Content-ID", "discontent ID"));
        final FeedEntityHeader entityHeader = new FeedEntityHeader(
            Timestamp.of(Instant.parse("2023-10-05T09:56:07.999Z")),
            OperationType.DELETE,
            ContentId.of("abcd"),
            extraHeaders);

        assertThat(entityHeader.toHttpHeaders()).isEqualTo(HttpHeaders.of(
            HttpHeader.of("Last-Modified", "Thu, 05 Oct 2023 09:56:07 GMT"),
            HttpHeader.of("Operation-Type", "http-equiv=DELETE"),
            HttpHeader.of("Content-ID", "abcd")));
    }
}
