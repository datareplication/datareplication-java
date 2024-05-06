package io.datareplication.model.feed;

import io.datareplication.model.HttpHeader;
import io.datareplication.model.HttpHeaders;
import io.datareplication.model.Url;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class FeedPageHeaderTest {
    @Test
    void shouldDefaultToEmptyExtraHeaders() {
        final FeedPageHeader header = new FeedPageHeader(
            Instant.now(),
            Link.self(Url.of("https://example.datareplication.io/page")),
            Optional.empty(),
            Optional.empty());

        assertThat(header.extraHeaders()).isEqualTo(HttpHeaders.EMPTY);
    }

    @Test
    void shouldReturnHttpHeaders() {
        final HttpHeaders extraHeaders = HttpHeaders.of(HttpHeader.of("e1", "v1"));
        final FeedPageHeader pageHeader = new FeedPageHeader(
            Instant.parse("2023-10-05T09:58:59.000Z"),
            Link.self(Url.of("https://example.datareplication.io/2")),
            Optional.of(Link.prev(Url.of("https://example.datareplication.io/1"))),
            Optional.of(Link.next(Url.of("https://example.datareplication.io/3"))),
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
            Instant.parse("2023-12-31T23:59:59.999Z"),
            Link.self(Url.of("https://example.datareplication.io/1")),
            Optional.empty(),
            Optional.empty(),
            HttpHeaders.EMPTY);

        assertThat(pageHeader.toHttpHeaders()).isEqualTo(HttpHeaders.of(
            HttpHeader.of("Last-Modified", "Sun, 31 Dec 2023 23:59:59 GMT"),
            HttpHeader.of("Link", "<https://example.datareplication.io/1>; rel=self")));
    }
}
