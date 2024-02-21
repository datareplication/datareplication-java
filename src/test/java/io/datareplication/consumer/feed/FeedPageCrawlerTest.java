package io.datareplication.consumer.feed;

import io.datareplication.consumer.HttpException;
import io.datareplication.model.HttpHeaders;
import io.datareplication.model.Timestamp;
import io.datareplication.model.Url;
import io.datareplication.model.feed.FeedPageHeader;
import io.datareplication.model.feed.Link;
import lombok.NonNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FeedPageCrawlerTest {

    @InjectMocks
    private FeedPageCrawler feedPageCrawler;

    @Mock
    private HeaderLoader headerLoader;

    private final FeedPageHeader pageHeader1 = new FeedPageHeader(
        Timestamp.of(Instant.parse("2023-10-01T09:58:59.000Z")),
        Link.self(Url.of("https://example.datareplication.io/1")),
        Optional.empty(),
        Optional.of(Link.next(Url.of("https://example.datareplication.io/2"))),
        HttpHeaders.EMPTY
    );

    private final FeedPageHeader pageHeader2 = new FeedPageHeader(
        Timestamp.of(Instant.parse("2023-10-02T09:58:59.000Z")),
        Link.self(Url.of("https://example.datareplication.io/2")),
        Optional.of(Link.prev(Url.of("https://example.datareplication.io/1"))),
        Optional.of(Link.next(Url.of("https://example.datareplication.io/3"))),
        HttpHeaders.EMPTY
    );

    private final FeedPageHeader pageHeader3 = new FeedPageHeader(
        Timestamp.of(Instant.parse("2023-10-03T09:58:59.000Z")),
        Link.self(Url.of("https://example.datareplication.io/3")),
        Optional.of(Link.prev(Url.of("https://example.datareplication.io/2"))),
        Optional.empty(),
        HttpHeaders.EMPTY
    );

    @BeforeEach
    void setUp() {
        lenient()
            .when(headerLoader.load(pageHeader1.self().value()))
            .thenReturn(CompletableFuture.supplyAsync(() -> pageHeader1));
        lenient()
            .when(headerLoader.load(pageHeader2.self().value()))
            .thenReturn(CompletableFuture.supplyAsync(() -> pageHeader2));
        lenient()
            .when(headerLoader.load(pageHeader3.self().value()))
            .thenReturn(CompletableFuture.supplyAsync(() -> pageHeader3));
    }

    @Test
    void startFromBeginningWithoutPrevLink_shouldStartWithThisPage() {
        Url url = pageHeader1.self().value();

        CompletionStage<@NonNull FeedPageHeader> result = feedPageCrawler.crawl(url, StartFrom.beginning());

        assertThat(result.toCompletableFuture()).isCompletedWithValue(pageHeader1);
    }

    @Test
    void crawlToBeginning_shouldStartWithPageHeader1() {
        Url url = pageHeader3.self().value();

        CompletionStage<@NonNull FeedPageHeader> result = feedPageCrawler.crawl(url, StartFrom.beginning());

        assertThat(result.toCompletableFuture()).isCompletedWithValue(pageHeader1);
    }

    @Test
    void crawlToBeginning_shouldThrowExceptionOnUnknownUrl() {
        Url url = Url.of("https://example.datareplication.io/unknown");
        HttpException.ClientError expectedException = new HttpException.ClientError(url, 404);
        when(headerLoader.load(url)).thenReturn(CompletableFuture.failedFuture(expectedException));

        CompletionStage<@NonNull FeedPageHeader> result = feedPageCrawler.crawl(url, StartFrom.beginning());

        assertThat(result.toCompletableFuture())
            .isCompletedExceptionally()
            .failsWithin(10, TimeUnit.MILLISECONDS)
            .withThrowableOfType(ExecutionException.class)
            .withCause(expectedException);
    }

    // TODO: Implement other StartFrom tests
}
