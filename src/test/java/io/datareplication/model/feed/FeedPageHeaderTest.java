package io.datareplication.model.feed;

import io.datareplication.model.HttpHeader;
import io.datareplication.model.HttpHeaders;
import io.datareplication.model.Timestamp;
import io.datareplication.model.Url;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class FeedPageHeaderTest {
    @Test
    void shouldReturnHttpHeaders() {
        final HttpHeaders extraHeaders = HttpHeaders.of(HttpHeader.of("e1", "v1"));
        final FeedPageHeader pageHeader = new FeedPageHeader(
            Timestamp.of(Instant.parse("2023-10-05T09:58:59.000Z")),
            Link.of(Url.of("https://example.datareplication.io/2")),
            Optional.of(Link.of(Url.of("https://example.datareplication.io/1"))),
            Optional.of(Link.of(Url.of("https://example.datareplication.io/3"))),
            extraHeaders);

        assertThat(pageHeader.toHttpHeaders()).isEqualTo(HttpHeaders.of(
            HttpHeader.of("Last-Modified", "Thu, 05 Oct 2023 09:58:59 GMT"),
            HttpHeader.of("Link", List.of(
                "<https://example.datareplication.io/2>; rel=self",
                "<https://example.datareplication.io/1>; rel=prev",
                "<https://example.datareplication.io/3>; rel=next")),
            HttpHeader.of("e1", "v1")));
    }

    @Test
    void shouldReturnHttpHeadersWithoutOptionalLinks() {
        final FeedPageHeader pageHeader = new FeedPageHeader(
            Timestamp.of(Instant.parse("2023-12-31T23:59:59.999Z")),
            Link.of(Url.of("https://example.datareplication.io/1")),
            Optional.empty(),
            Optional.empty(),
            HttpHeaders.EMPTY);

        assertThat(pageHeader.toHttpHeaders()).isEqualTo(HttpHeaders.of(
            HttpHeader.of("Last-Modified", "Sun, 31 Dec 2023 23:59:59 GMT"),
            HttpHeader.of("Link", "<https://example.datareplication.io/1>; rel=self")));
    }
}
