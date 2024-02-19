package io.datareplication.producer.feed;

import io.datareplication.model.*;
import io.datareplication.model.feed.FeedPageHeader;
import io.datareplication.model.feed.Link;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class HeaderAndContentTypeTest {
    @Test
    void shouldReturnHttpHeaders() {
        var header = new FeedPageProvider.HeaderAndContentType(
            new FeedPageHeader(
                Timestamp.of(Instant.parse("2024-02-02T15:56:31Z")),
                Link.self(Url.of("self-link")),
                Optional.of(Link.prev(Url.of("prev-link"))),
                Optional.of(Link.next(Url.of("next-link"))),
                HttpHeaders.of(
                    HttpHeader.of("h1", "v1"),
                    HttpHeader.of("h2", "v2")
                )
            ),
            ContentType.of("audio/ogg")
        );

        var result = header.toHttpHeaders();

        assertThat(result).isEqualTo(HttpHeaders.of(
            HttpHeader.lastModified(header.header().lastModified()),
            HttpHeader.of(HttpHeader.LINK, List.of(
                "<self-link>; rel=self",
                "<prev-link>; rel=prev",
                "<next-link>; rel=next"
            )),
            HttpHeader.of("h1", "v1"),
            HttpHeader.of("h2", "v2"),
            HttpHeader.contentType(header.contentType())
        ));
    }
}
