package io.datareplication.consumer.feed;

import io.datareplication.consumer.ContentIdNotFoundException;
import io.datareplication.consumer.HttpException;
import io.datareplication.consumer.StreamingPage;
import io.datareplication.consumer.TestStreamingPage.TestEntityParts;
import io.datareplication.internal.page.PageLoader;
import io.datareplication.model.Body;
import io.datareplication.model.BodyTestUtil;
import io.datareplication.model.ContentType;
import io.datareplication.model.Entity;
import io.datareplication.model.HttpHeader;
import io.datareplication.model.HttpHeaders;
import io.datareplication.model.Url;
import io.datareplication.model.feed.ContentId;
import io.datareplication.model.feed.FeedEntityHeader;
import io.datareplication.model.feed.FeedPageHeader;
import io.datareplication.model.feed.Link;
import io.datareplication.model.feed.OperationType;
import lombok.NonNull;
import org.assertj.core.api.ListAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.adapter.JdkFlowAdapter;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static io.datareplication.consumer.TestStreamingPage.testStreamingPageOf;
import static java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class
FeedConsumerImplTest {
    @Mock
    private FeedPageCrawler feedPageCrawler;
    @Mock
    private PageLoader pageLoader;
    @Mock
    private FeedPageHeaderParser feedPageHeaderParser;
    @InjectMocks
    private FeedConsumerImpl feedConsumer;

    private static final String BOUNDARY = "boundary";
    private static final Instant LAST_MODIFIED =
        Instant.from(RFC_1123_DATE_TIME.parse("Thu, 5 Oct 2023 03:00:14 GMT"));
    private static final Instant LAST_MODIFIED_AFTER =
        Instant.from(RFC_1123_DATE_TIME.parse("Thu, 5 Oct 2023 03:00:15 GMT"));
    private static final Instant LAST_MODIFIED_BEFORE =
        Instant.from(RFC_1123_DATE_TIME.parse("Thu, 5 Oct 2023 03:00:13 GMT"));
    private static final HttpHeaders ENTITY_HTTP_HEADERS_1 =
        HttpHeaders.of(HttpHeader.of("entity", "first"));
    private static final HttpHeaders ENTITY_HTTP_HEADERS_2 =
        HttpHeaders.of(HttpHeader.of("entity", "second"));
    private static final HttpHeaders ENTITY_HTTP_HEADERS_3 =
        HttpHeaders.of(HttpHeader.of("entity", "third"));
    private static final HttpHeaders PAGE_HTTP_HEADERS_1 =
        HttpHeaders.of(HttpHeader.of("page", "first"));
    private static final HttpHeaders PAGE_HTTP_HEADERS_2 =
        HttpHeaders.of(HttpHeader.of("page", "second"));
    private static final HttpHeaders PAGE_HTTP_HEADERS_3 =
        HttpHeaders.of(HttpHeader.of("page", "third"));
    private static final Url URL_1 = Url.of("url1");
    private static final Url URL_2 = Url.of("url2");
    private static final Url URL_3 = Url.of("url3");
    private static final ContentId CONTENT_ID_1 = ContentId.of("contentId1");
    private static final ContentId CONTENT_ID_2 = ContentId.of("contentId2");
    private static final ContentId CONTENT_ID_3 = ContentId.of("contentId3");
    private static final FeedEntityHeader FEED_ENTITY_HEADER_1 =
        new FeedEntityHeader(LAST_MODIFIED, OperationType.PUT, CONTENT_ID_1);
    private static final FeedEntityHeader FEED_ENTITY_HEADER_2 =
        new FeedEntityHeader(LAST_MODIFIED, OperationType.PUT, CONTENT_ID_2);
    private static final FeedEntityHeader FEED_ENTITY_HEADER_3 =
        new FeedEntityHeader(LAST_MODIFIED, OperationType.PUT, CONTENT_ID_2);

    @BeforeEach
    void setUp() {
        FeedPageHeader feedPageHeader1 = new FeedPageHeader(
            LAST_MODIFIED,
            Link.self(URL_1),
            Optional.empty(),
            Optional.of(Link.next(URL_2))
        );
        FeedPageHeader feedPageHeader2 = new FeedPageHeader(
            LAST_MODIFIED,
            Link.self(URL_2),
            Optional.of(Link.prev(URL_1)),
            Optional.of(Link.next(URL_3))
        );
        FeedPageHeader feedPageHeader3 = new FeedPageHeader(
            LAST_MODIFIED,
            Link.self(URL_3),
            Optional.of(Link.prev(URL_2)),
            Optional.empty()
        );
        lenient()
            .when(feedPageHeaderParser.feedPageHeader(PAGE_HTTP_HEADERS_1))
            .thenReturn(feedPageHeader1);
        lenient()
            .when(feedPageHeaderParser.feedPageHeader(PAGE_HTTP_HEADERS_2))
            .thenReturn(feedPageHeader2);
        lenient()
            .when(feedPageHeaderParser.feedPageHeader(PAGE_HTTP_HEADERS_3))
            .thenReturn(feedPageHeader3);
    }

    @Test
    void streamPagesFromBeginning_shouldConsumeTheOneAndOnlyPage() {
        FeedPageHeader feedPageHeader = new FeedPageHeader(
            LAST_MODIFIED,
            Link.self(URL_1),
            Optional.empty(),
            Optional.empty()
        );
        var page1 = testStreamingPageOf(
            PAGE_HTTP_HEADERS_1,
            BOUNDARY,
            TestEntityParts.of(ENTITY_HTTP_HEADERS_1, "Hello World!")
        );

        when(feedPageHeaderParser.feedPageHeader(PAGE_HTTP_HEADERS_1)).thenReturn(feedPageHeader);
        when(feedPageHeaderParser.feedEntityHeader(0, ENTITY_HTTP_HEADERS_1)).thenReturn(FEED_ENTITY_HEADER_1);
        when(feedPageCrawler.crawl(URL_1, StartFrom.beginning())).thenReturn(Mono.just(URL_1));
        when(pageLoader.load(URL_1)).thenReturn(Mono.just(page1));

        List<@NonNull StreamingPage<@NonNull FeedPageHeader, @NonNull FeedEntityHeader>> pages = JdkFlowAdapter
            .flowPublisherToFlux(feedConsumer.streamPages(URL_1, StartFrom.beginning()))
            .collectList()
            .single()
            .block();

        assertEntities(pages)
            .hasSize(1)
            .containsExactly(
                new Entity<>(FEED_ENTITY_HEADER_1, Body.fromUtf8("Hello World!", ContentType.of("text/plain")))
            );
    }

    @Test
    void streamPagesFromBeginning_shouldFollowAndConsumeNextLinks() {
        var page1 = testStreamingPageOf(
            PAGE_HTTP_HEADERS_1,
            BOUNDARY,
            TestEntityParts.of(ENTITY_HTTP_HEADERS_1, "first entity")
        );
        var page2 = testStreamingPageOf(
            PAGE_HTTP_HEADERS_2,
            BOUNDARY,
            TestEntityParts.of(ENTITY_HTTP_HEADERS_2, "second entity")
        );
        var page3 = testStreamingPageOf(
            PAGE_HTTP_HEADERS_3,
            BOUNDARY,
            TestEntityParts.of(ENTITY_HTTP_HEADERS_3, "third entity")
        );
        when(feedPageCrawler.crawl(URL_3, StartFrom.beginning())).thenReturn(Mono.just(URL_1));
        when(feedPageHeaderParser.feedEntityHeader(0, ENTITY_HTTP_HEADERS_1)).thenReturn(FEED_ENTITY_HEADER_1);
        when(feedPageHeaderParser.feedEntityHeader(0, ENTITY_HTTP_HEADERS_2)).thenReturn(FEED_ENTITY_HEADER_2);
        when(feedPageHeaderParser.feedEntityHeader(0, ENTITY_HTTP_HEADERS_3)).thenReturn(FEED_ENTITY_HEADER_3);
        when(pageLoader.load(URL_1)).thenReturn(Mono.just(page1));
        when(pageLoader.load(URL_2)).thenReturn(Mono.just(page2));
        when(pageLoader.load(URL_3)).thenReturn(Mono.just(page3));

        List<@NonNull StreamingPage<@NonNull FeedPageHeader, @NonNull FeedEntityHeader>> pages = JdkFlowAdapter
            .flowPublisherToFlux(feedConsumer.streamPages(URL_3, StartFrom.beginning()))
            .collectList()
            .single()
            .block();

        assertEntities(pages)
            .hasSize(3)
            .containsExactly(
                new Entity<>(FEED_ENTITY_HEADER_1, Body.fromUtf8("first entity", ContentType.of("text/plain"))),
                new Entity<>(FEED_ENTITY_HEADER_2, Body.fromUtf8("second entity", ContentType.of("text/plain"))),
                new Entity<>(FEED_ENTITY_HEADER_3, Body.fromUtf8("third entity", ContentType.of("text/plain")))
            );
    }

    @Test
    void streamPage_shouldContainTheSameEntitiesAsStreamEntities() {
        when(feedPageCrawler.crawl(URL_3, StartFrom.beginning())).thenReturn(Mono.just(URL_1));
        when(feedPageHeaderParser.feedEntityHeader(0, ENTITY_HTTP_HEADERS_1)).thenReturn(FEED_ENTITY_HEADER_1);
        when(feedPageHeaderParser.feedEntityHeader(0, ENTITY_HTTP_HEADERS_2)).thenReturn(FEED_ENTITY_HEADER_2);
        when(feedPageHeaderParser.feedEntityHeader(0, ENTITY_HTTP_HEADERS_3)).thenReturn(FEED_ENTITY_HEADER_3);

        preparePageToStream();
        var pageEntities = JdkFlowAdapter
            .flowPublisherToFlux(feedConsumer.streamPages(URL_3, StartFrom.beginning()))
            .flatMap(page -> JdkFlowAdapter.flowPublisherToFlux(page.toCompleteEntities()))
            .collectList()
            .single()
            .block();
        preparePageToStream();
        var entities = JdkFlowAdapter
            .flowPublisherToFlux(feedConsumer.streamEntities(URL_3, StartFrom.beginning()))
            .collectList()
            .single()
            .block();

        assertThat(entities).isEqualTo(pageEntities);
    }

    /**
     * Must be called before {@link #streamPage_shouldContainTheSameEntitiesAsStreamEntities()}.
     * A stream can only be consumed once.
     */
    private void preparePageToStream() {
        var page1 = testStreamingPageOf(
            PAGE_HTTP_HEADERS_1,
            BOUNDARY,
            TestEntityParts.of(ENTITY_HTTP_HEADERS_1, "first entity")
        );
        var page2 = testStreamingPageOf(
            PAGE_HTTP_HEADERS_2,
            BOUNDARY,
            TestEntityParts.of(ENTITY_HTTP_HEADERS_2, "second entity")
        );
        var page3 = testStreamingPageOf(
            PAGE_HTTP_HEADERS_3,
            BOUNDARY,
            TestEntityParts.of(ENTITY_HTTP_HEADERS_3, "third entity")
        );
        when(pageLoader.load(URL_1)).thenReturn(Mono.just(page1));
        when(pageLoader.load(URL_2)).thenReturn(Mono.just(page2));
        when(pageLoader.load(URL_3)).thenReturn(Mono.just(page3));
    }

    @Test
    void crawlPages_shouldThrowHttpException_fromUnderlyingHttpClient() {
        HttpException.NetworkError expectedNetworkError = new HttpException.NetworkError(URL_1, new IOException());
        when(feedPageCrawler.crawl(URL_1, StartFrom.beginning())).thenReturn(Mono.error(expectedNetworkError));

        final var result = JdkFlowAdapter
            .flowPublisherToFlux(feedConsumer.streamPages(URL_1, StartFrom.beginning()));

        StepVerifier
            .create(result)
            .expectNextCount(0)
            .expectErrorMatches(expectedNetworkError::equals)
            .verify();
    }

    @Test
    void loadPage_shouldThrowHttpException_fromUnderlyingHttpClient() {
        HttpException.NetworkError expectedNetworkError = new HttpException.NetworkError(URL_1, new IOException());
        when(feedPageCrawler.crawl(URL_1, StartFrom.beginning())).thenReturn(Mono.just(URL_1));
        when(pageLoader.load(URL_1)).thenReturn(Mono.error(expectedNetworkError));

        final var result = JdkFlowAdapter
            .flowPublisherToFlux(feedConsumer.streamPages(URL_1, StartFrom.beginning()));

        StepVerifier
            .create(result)
            .expectNextCount(0)
            .expectErrorMatches(expectedNetworkError::equals)
            .verify();
    }

    @Test
    void streamEntitiesFromTimestamp_shouldOnlyConsumeNewerEntities() {
        StartFrom startFrom = StartFrom.timestamp(LAST_MODIFIED);
        FeedPageHeader feedPageHeader1 = new FeedPageHeader(
            LAST_MODIFIED,
            Link.self(URL_1),
            Optional.empty(),
            Optional.empty()
        );
        var page1 = testStreamingPageOf(
            PAGE_HTTP_HEADERS_1,
            BOUNDARY,
            TestEntityParts.of(ENTITY_HTTP_HEADERS_1, "already consumed older timestamp"),
            TestEntityParts.of(ENTITY_HTTP_HEADERS_2, "new entity"),
            TestEntityParts.of(ENTITY_HTTP_HEADERS_3, "newer entity")
        );
        FeedEntityHeader feedEntityHeader = new FeedEntityHeader(LAST_MODIFIED_AFTER, OperationType.PUT, CONTENT_ID_3);

        when(feedPageHeaderParser.feedPageHeader(PAGE_HTTP_HEADERS_1)).thenReturn(feedPageHeader1);
        when(feedPageHeaderParser.feedEntityHeader(0, ENTITY_HTTP_HEADERS_1))
            .thenReturn(new FeedEntityHeader(LAST_MODIFIED_BEFORE, OperationType.PUT, CONTENT_ID_1));
        when(feedPageHeaderParser.feedEntityHeader(1, ENTITY_HTTP_HEADERS_2))
            .thenReturn(new FeedEntityHeader(LAST_MODIFIED, OperationType.PUT, CONTENT_ID_2));
        when(feedPageHeaderParser.feedEntityHeader(2, ENTITY_HTTP_HEADERS_3))
            .thenReturn(feedEntityHeader);
        when(feedPageCrawler.crawl(URL_1, startFrom)).thenReturn(Mono.just(URL_1));
        when(pageLoader.load(URL_1)).thenReturn(Mono.just(page1));

        List<@NonNull Entity<@NonNull FeedEntityHeader>> entities = JdkFlowAdapter
            .flowPublisherToFlux(feedConsumer.streamEntities(URL_1, startFrom))
            .collectList()
            .single()
            .block();

        assertThat(entities)
            .hasSize(2)
            .extracting(entity -> entity.body().toUtf8())
            .containsExactly("new entity", "newer entity");
    }

    @Test
    void streamEntitiesFromContentId_shouldOnlyConsumeNewerEntities() {
        StartFrom startFrom = StartFrom.contentId(CONTENT_ID_2, LAST_MODIFIED);
        FeedPageHeader feedPageHeader1 = new FeedPageHeader(
            LAST_MODIFIED,
            Link.self(URL_1),
            Optional.empty(),
            Optional.empty()
        );
        var page1 = testStreamingPageOf(
            PAGE_HTTP_HEADERS_1,
            BOUNDARY,
            TestEntityParts.of(ENTITY_HTTP_HEADERS_1, "already consumed older timestamp"),
            TestEntityParts.of(ENTITY_HTTP_HEADERS_2, "already consumed ContentId"),
            TestEntityParts.of(ENTITY_HTTP_HEADERS_3, "new entity")
        );
        when(feedPageHeaderParser.feedPageHeader(PAGE_HTTP_HEADERS_1)).thenReturn(feedPageHeader1);
        when(feedPageHeaderParser.feedEntityHeader(0, ENTITY_HTTP_HEADERS_1))
            .thenReturn(new FeedEntityHeader(LAST_MODIFIED_BEFORE, OperationType.PUT, CONTENT_ID_1));
        when(feedPageHeaderParser.feedEntityHeader(1, ENTITY_HTTP_HEADERS_2))
            .thenReturn(new FeedEntityHeader(LAST_MODIFIED, OperationType.PUT, CONTENT_ID_2));
        when(feedPageHeaderParser.feedEntityHeader(2, ENTITY_HTTP_HEADERS_3))
            .thenReturn(new FeedEntityHeader(LAST_MODIFIED_AFTER, OperationType.PUT, CONTENT_ID_3));
        when(feedPageCrawler.crawl(URL_1, startFrom)).thenReturn(Mono.just(URL_1));
        when(pageLoader.load(URL_1)).thenReturn(Mono.just(page1));

        List<@NonNull Entity<@NonNull FeedEntityHeader>> entities = JdkFlowAdapter
            .flowPublisherToFlux(feedConsumer.streamEntities(URL_1, startFrom))
            .collectList()
            .single()
            .block();

        assertThat(entities)
            .hasSize(1)
            .extracting(entity -> entity.body().toUtf8())
            .containsExactly("new entity");
    }

    @Test
    void streamEntitiesFromContentId_shouldThrowExceptionWhenThereIsNoMatchingStartingPage() {
        var startFrom = StartFrom.contentId(ContentId.of("Unknown"), LAST_MODIFIED);
        ContentIdNotFoundException contentIdNotFoundException = new ContentIdNotFoundException(
            startFrom, URL_1, LAST_MODIFIED_AFTER
        );

        FeedPageHeader feedPageHeader1 = new FeedPageHeader(
            LAST_MODIFIED,
            Link.self(URL_1),
            Optional.empty(),
            Optional.empty()
        );
        var page1 = testStreamingPageOf(
            PAGE_HTTP_HEADERS_1,
            BOUNDARY,
            TestEntityParts.of(ENTITY_HTTP_HEADERS_1, "before"),
            TestEntityParts.of(ENTITY_HTTP_HEADERS_2, "matches last modified"),
            TestEntityParts.of(ENTITY_HTTP_HEADERS_3, "after last modified -> should result into exception")
        );
        when(feedPageHeaderParser.feedPageHeader(PAGE_HTTP_HEADERS_1)).thenReturn(feedPageHeader1);
        when(feedPageHeaderParser.feedEntityHeader(0, ENTITY_HTTP_HEADERS_1))
            .thenReturn(new FeedEntityHeader(LAST_MODIFIED_BEFORE, OperationType.PUT, CONTENT_ID_1));
        when(feedPageHeaderParser.feedEntityHeader(1, ENTITY_HTTP_HEADERS_2))
            .thenReturn(new FeedEntityHeader(LAST_MODIFIED, OperationType.PUT, CONTENT_ID_2));
        when(feedPageHeaderParser.feedEntityHeader(2, ENTITY_HTTP_HEADERS_3))
            .thenReturn(new FeedEntityHeader(LAST_MODIFIED_AFTER, OperationType.PUT, CONTENT_ID_3));
        when(feedPageCrawler.crawl(URL_1, startFrom)).thenReturn(Mono.just(URL_1));
        when(pageLoader.load(URL_1)).thenReturn(Mono.just(page1));

        final var result = JdkFlowAdapter
            .flowPublisherToFlux(feedConsumer.streamEntities(URL_1, startFrom));

        StepVerifier
            .create(result)
            .expectNextCount(0)
            .expectErrorMatches(contentIdNotFoundException::equals)
            .verify();
    }

    private static ListAssert<@NonNull Entity<@NonNull FeedEntityHeader>>
    assertEntities(final List<@NonNull StreamingPage<@NonNull FeedPageHeader, @NonNull FeedEntityHeader>> pages) {
        return assertThat(
            Flux
                .fromIterable(pages)
                .flatMap(page -> JdkFlowAdapter.flowPublisherToFlux(page.toCompleteEntities()))
                .collectList()
                .single()
                .block()
        )
            .usingRecursiveFieldByFieldElementComparator(BodyTestUtil.bodyContentsComparator());
    }


}
