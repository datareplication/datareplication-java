package io.datareplication.consumer.feed;

import io.datareplication.internal.http.HttpClient;
import io.datareplication.model.HttpHeader;
import io.datareplication.model.HttpHeaders;
import io.datareplication.model.Timestamp;
import io.datareplication.model.Url;
import io.datareplication.model.feed.FeedPageHeader;
import io.datareplication.model.feed.Link;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HeaderLoaderTest {

    @InjectMocks
    private HeaderLoader headerLoader;

    @Mock
    private FeedPageHeaderParser parser;

    @Mock
    private HttpClient httpClient;

    @Mock
    private HttpResponse<Void> response;

    private final Url url = Url.of("https://example.datareplication.io/1");

    private final FeedPageHeader expectedFeedHeader = new FeedPageHeader(
        Timestamp.of(Instant.parse("2023-10-02T09:58:59.000Z")),
        Link.self(Url.of("https://example.datareplication.io/1")),
        Optional.empty(),
        Optional.empty(),
        HttpHeaders.EMPTY
    );

    private final java.net.http.HttpHeaders javaHttpHeaders = java.net.http.HttpHeaders
        .of(Map.of("foo", List.of("bar")), (s1, s2) -> true);
    private final HttpHeaders httpHeaders = HttpHeaders.of(HttpHeader.of("foo", "bar"));

    @BeforeEach
    void setUp() {
        when(response.headers()).thenReturn(javaHttpHeaders);
    }

    @Test
    void shouldReturnParsedHeaders() {
        when(httpClient.head(url)).thenReturn(Mono.just(response));
        when(parser.feedPageHeader(httpHeaders)).thenReturn(expectedFeedHeader);

        FeedPageHeader result = headerLoader
            .load(url)
            .single()
            .block();

        assertThat(result).isEqualTo(expectedFeedHeader);
    }

    // TODO: Parsing error
}
