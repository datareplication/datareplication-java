package io.datareplication.consumer.feed;

import io.datareplication.consumer.HttpException;
import io.datareplication.consumer.PageFormatException;
import io.datareplication.model.HttpHeaders;
import io.datareplication.model.Url;
import io.datareplication.model.feed.ContentId;
import io.datareplication.model.feed.FeedPageHeader;
import io.datareplication.model.feed.Link;
import lombok.NonNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Optional;
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

    private static final Instant INSTANT_PAGE_1 = Instant.parse("2023-10-01T00:00:01.000Z");
    private static final Instant INSTANT_PAGE_2 = Instant.parse("2023-10-01T00:00:02.000Z");
    private static final Instant INSTANT_PAGE_3 = Instant.parse("2023-10-01T00:00:03.000Z");

    private final FeedPageHeader pageHeader1 = new FeedPageHeader(
        INSTANT_PAGE_1,
        Link.self(Url.of("https://example.datareplication.io/1")),
        Optional.empty(),
        Optional.of(Link.next(Url.of("https://example.datareplication.io/2"))),
        HttpHeaders.EMPTY
    );

    private final FeedPageHeader pageHeader2 = new FeedPageHeader(
        INSTANT_PAGE_2,
        Link.self(Url.of("https://example.datareplication.io/2")),
        Optional.of(Link.prev(Url.of("https://example.datareplication.io/1"))),
        Optional.of(Link.next(Url.of("https://example.datareplication.io/3"))),
        HttpHeaders.EMPTY
    );

    private final FeedPageHeader pageHeader2NoNextLink = new FeedPageHeader(
        INSTANT_PAGE_2,
        Link.self(Url.of("https://example.datareplication.io/2")),
        Optional.of(Link.prev(Url.of("https://example.datareplication.io/1"))),
        Optional.empty(),
        HttpHeaders.EMPTY
    );


    private final FeedPageHeader pageHeader3 = new FeedPageHeader(
        INSTANT_PAGE_3,
        Link.self(Url.of("https://example.datareplication.io/3")),
        Optional.of(Link.prev(Url.of("https://example.datareplication.io/2"))),
        Optional.empty(),
        HttpHeaders.EMPTY
    );

    @BeforeEach
    void setUp() {
        lenient()
            .when(headerLoader.load(pageHeader1.self().value()))
            .thenReturn(Mono.just(pageHeader1));
        lenient()
            .when(headerLoader.load(pageHeader2.self().value()))
            .thenReturn(Mono.just(pageHeader2));
        lenient()
            .when(headerLoader.load(pageHeader3.self().value()))
            .thenReturn(Mono.just(pageHeader3));
    }

    @Test
    void startFromBeginningWithoutPrevLink_shouldStartWithThisPage() {
        Url url = pageHeader1.self().value();

        Mono<@NonNull Url> result = feedPageCrawler.crawl(url, StartFrom.beginning());

        assertThat(result.toFuture()).isCompletedWithValue(pageHeader1.self().value());
    }

    @Test
    void crawlToBeginning_shouldStartWithPageHeader1() {
        Url url = pageHeader3.self().value();

        Mono<@NonNull Url> result = feedPageCrawler.crawl(url, StartFrom.beginning());

        assertThat(result.toFuture()).isCompletedWithValue(pageHeader1.self().value());
    }

    @Test
    void crawlToBeginning_shouldThrowExceptionOnUnknownUrl() {
        Url url = Url.of("https://example.datareplication.io/unknown");
        HttpException.ClientError expectedException = new HttpException.ClientError(url, 404);
        when(headerLoader.load(url)).thenReturn(Mono.error(expectedException));

        Mono<@NonNull Url> result = feedPageCrawler.crawl(url, StartFrom.beginning());

        assertThat(result.toFuture())
            .isCompletedExceptionally()
            .failsWithin(10, TimeUnit.MILLISECONDS)
            .withThrowableOfType(ExecutionException.class)
            .withCause(expectedException);
    }

    @Test
    void startFromTimestamp_shouldStartWithPage2() {
        Url url = pageHeader1.self().value();

        Mono<@NonNull Url> result = feedPageCrawler.crawl(url, StartFrom.timestamp(INSTANT_PAGE_2));

        assertThat(result.toFuture()).isCompletedWithValue(pageHeader2.self().value());
    }

    @Test
    void startFromContentId_shouldStartWithPage2() {
        Url url = pageHeader1.self().value();

        Mono<@NonNull Url> result =
            feedPageCrawler.crawl(url, StartFrom.contentId(ContentId.of("any id"), INSTANT_PAGE_2));

        assertThat(result.toFuture()).isCompletedWithValue(pageHeader2.self().value());
    }

    @Test
    void startFromTimestamp_withNoOlderPage_shouldThrowException() {
        Url url = pageHeader1.self().value();

        Mono<@NonNull Url> result = feedPageCrawler.crawl(url, StartFrom.timestamp(INSTANT_PAGE_1));

        assertThat(result.toFuture())
            .isCompletedExceptionally()
            .failsWithin(10, TimeUnit.MILLISECONDS)
            .withThrowableOfType(ExecutionException.class)
            .withCauseInstanceOf(FeedException.FeedNotOldEnough.class)
            .withCause(new FeedException.FeedNotOldEnough(pageHeader1.self().value(), INSTANT_PAGE_1, INSTANT_PAGE_1));
    }

    @Test
    void startFromTimestamp_withNoNextLinkOnOlderPage_shouldThrowException() {
        when(headerLoader.load(pageHeader2.self().value()))
            .thenReturn(Mono.just(pageHeader2NoNextLink));

        Url url = pageHeader3.self().value();

        Mono<@NonNull Url> result = feedPageCrawler.crawl(url, StartFrom.timestamp(INSTANT_PAGE_3));

        assertThat(result.toFuture())
            .isCompletedExceptionally()
            .failsWithin(10, TimeUnit.MILLISECONDS)
            .withThrowableOfType(ExecutionException.class)
            .withCauseInstanceOf(PageFormatException.MissingNextLinkHeader.class)
            .withCause(new PageFormatException.MissingNextLinkHeader(pageHeader2NoNextLink.toHttpHeaders()));
    }

    @Test
    void startFromContentId_withNoOlderPage_shouldThrowException() {
        Url url = pageHeader1.self().value();

        Mono<@NonNull Url> result =
            feedPageCrawler.crawl(url, StartFrom.contentId(ContentId.of("any id"), INSTANT_PAGE_1));

        assertThat(result.toFuture())
            .isCompletedExceptionally()
            .failsWithin(10, TimeUnit.MILLISECONDS)
            .withThrowableOfType(ExecutionException.class)
            .withCauseInstanceOf(FeedException.FeedNotOldEnough.class)
            .withCause(new FeedException.FeedNotOldEnough(pageHeader1.self().value(), INSTANT_PAGE_1, INSTANT_PAGE_1));
    }
}
