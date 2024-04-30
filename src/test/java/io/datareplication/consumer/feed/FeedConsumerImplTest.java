package io.datareplication.consumer.feed;

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
import io.datareplication.model.Timestamp;
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
import java.util.List;
import java.util.Optional;

import static io.datareplication.consumer.TestStreamingPage.testStreamingPageOf;
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
    private Timestamp lastModified;
    private Timestamp lastModifiedAfter;
    private Timestamp lastModifiedBefore;
    private HttpHeaders defaultEntityHeaders1;
    private HttpHeaders defaultEntityHeaders2;
    private HttpHeaders defaultEntityHeaders3;
    private HttpHeaders defaultPagesHeaders1;
    private HttpHeaders defaultPagesHeaders2;
    private HttpHeaders defaultPagesHeaders3;
    private Url url1;
    private Url url2;
    private Url url3;
    private ContentId contentId1;
    private ContentId contentId2;
    private ContentId contentId3;
    private FeedEntityHeader feedEntityHeader1;
    private FeedEntityHeader feedEntityHeader2;
    private FeedEntityHeader feedEntityHeader3;
    private FeedPageHeader feedPageHeader1;
    private FeedPageHeader feedPageHeader2;
    private FeedPageHeader feedPageHeader3;

    @BeforeEach
    void setUp() {
        lastModified = Timestamp.fromRfc1123String("Thu, 5 Oct 2023 03:00:14 GMT");
        lastModifiedBefore = Timestamp.fromRfc1123String("Thu, 5 Oct 2023 03:00:13 GMT");
        lastModifiedAfter = Timestamp.fromRfc1123String("Thu, 5 Oct 2023 03:00:15 GMT");
        defaultEntityHeaders1 = HttpHeaders.of(HttpHeader.of("entity", "first"));
        defaultEntityHeaders2 = HttpHeaders.of(HttpHeader.of("entity", "second"));
        defaultEntityHeaders3 = HttpHeaders.of(HttpHeader.of("entity", "third"));
        defaultPagesHeaders1 = HttpHeaders.of(HttpHeader.of("page", "first"));
        defaultPagesHeaders2 = HttpHeaders.of(HttpHeader.of("page", "second"));
        defaultPagesHeaders3 = HttpHeaders.of(HttpHeader.of("page", "third"));
        url1 = Url.of("url1");
        url2 = Url.of("url2");
        url3 = Url.of("url3");
        contentId1 = ContentId.of("contentId1");
        contentId2 = ContentId.of("contentId2");
        contentId3 = ContentId.of("contentId3");
        feedEntityHeader1 = new FeedEntityHeader(lastModified, OperationType.PUT, contentId1);
        feedEntityHeader2 = new FeedEntityHeader(lastModified, OperationType.PUT, contentId2);
        feedEntityHeader3 = new FeedEntityHeader(lastModified, OperationType.PUT, contentId2);
        feedPageHeader1 = new FeedPageHeader(
            lastModified,
            Link.self(url1),
            Optional.empty(),
            Optional.of(Link.next(url2))
        );
        feedPageHeader2 = new FeedPageHeader(
            lastModified,
            Link.self(url2),
            Optional.of(Link.prev(url1)),
            Optional.of(Link.next(url3))
        );
        feedPageHeader3 = new FeedPageHeader(
            lastModified,
            Link.self(url3),
            Optional.of(Link.prev(url2)),
            Optional.empty()
        );
        lenient()
            .when(feedPageHeaderParser.feedPageHeader(defaultPagesHeaders1))
            .thenReturn(feedPageHeader1);
        lenient()
            .when(feedPageHeaderParser.feedPageHeader(defaultPagesHeaders2))
            .thenReturn(feedPageHeader2);
        lenient()
            .when(feedPageHeaderParser.feedPageHeader(defaultPagesHeaders3))
            .thenReturn(feedPageHeader3);
    }

    @Test
    void streamPagesFromBeginning_shouldConsumeTheOneAndOnlyPage() {
        FeedPageHeader feedPageHeader = new FeedPageHeader(
            lastModified,
            Link.self(url1),
            Optional.empty(),
            Optional.empty()
        );
        var page1 = testStreamingPageOf(
            defaultPagesHeaders1,
            "boundary-1",
            TestEntityParts.of(defaultEntityHeaders1, "Hello World!")
        );

        when(feedPageHeaderParser.feedPageHeader(defaultPagesHeaders1)).thenReturn(feedPageHeader);
        when(feedPageHeaderParser.feedEntityHeader(0, defaultEntityHeaders1)).thenReturn(feedEntityHeader1);
        when(feedPageCrawler.crawl(url1, StartFrom.beginning())).thenReturn(Mono.just(url1));
        when(pageLoader.load(url1)).thenReturn(Mono.just(page1));

        List<@NonNull StreamingPage<@NonNull FeedPageHeader, @NonNull FeedEntityHeader>> pages = JdkFlowAdapter
            .flowPublisherToFlux(feedConsumer.streamPages(url1, StartFrom.beginning()))
            .collectList()
            .single()
            .block();

        assertEntities(pages)
            .hasSize(1)
            .containsExactly(
                new Entity<>(feedEntityHeader1, Body.fromUtf8("Hello World!", ContentType.of("text/plain")))
            );
    }

    @Test
    void streamPagesFromBeginning_shouldFollowAndConsumeNextLinks() {
        var page1 = testStreamingPageOf(
            defaultPagesHeaders1,
            "boundary-1",
            TestEntityParts.of(defaultEntityHeaders1, "first entity")
        );
        var page2 = testStreamingPageOf(
            defaultPagesHeaders2,
            "boundary-1",
            TestEntityParts.of(defaultEntityHeaders2, "second entity")
        );
        var page3 = testStreamingPageOf(
            defaultPagesHeaders3,
            "boundary-1",
            TestEntityParts.of(defaultEntityHeaders3, "third entity")
        );
        when(feedPageCrawler.crawl(url3, StartFrom.beginning())).thenReturn(Mono.just(url1));
        when(feedPageHeaderParser.feedEntityHeader(0, defaultEntityHeaders1)).thenReturn(feedEntityHeader1);
        when(feedPageHeaderParser.feedEntityHeader(0, defaultEntityHeaders2)).thenReturn(feedEntityHeader2);
        when(feedPageHeaderParser.feedEntityHeader(0, defaultEntityHeaders3)).thenReturn(feedEntityHeader3);
        when(pageLoader.load(url1)).thenReturn(Mono.just(page1));
        when(pageLoader.load(url2)).thenReturn(Mono.just(page2));
        when(pageLoader.load(url3)).thenReturn(Mono.just(page3));

        List<@NonNull StreamingPage<@NonNull FeedPageHeader, @NonNull FeedEntityHeader>> pages = JdkFlowAdapter
            .flowPublisherToFlux(feedConsumer.streamPages(url3, StartFrom.beginning()))
            .collectList()
            .single()
            .block();

        assertEntities(pages)
            .hasSize(3)
            .containsExactly(
                new Entity<>(feedEntityHeader1, Body.fromUtf8("first entity", ContentType.of("text/plain"))),
                new Entity<>(feedEntityHeader2, Body.fromUtf8("second entity", ContentType.of("text/plain"))),
                new Entity<>(feedEntityHeader3, Body.fromUtf8("third entity", ContentType.of("text/plain")))
            );
    }

    @Test
    void streamPage_shouldContainTheSameEntitiesAsStreamEntities() {
        when(feedPageCrawler.crawl(url3, StartFrom.beginning())).thenReturn(Mono.just(url1));
        when(feedPageHeaderParser.feedEntityHeader(0, defaultEntityHeaders1)).thenReturn(feedEntityHeader1);
        when(feedPageHeaderParser.feedEntityHeader(0, defaultEntityHeaders2)).thenReturn(feedEntityHeader2);
        when(feedPageHeaderParser.feedEntityHeader(0, defaultEntityHeaders3)).thenReturn(feedEntityHeader3);

        preparePageToStream();
        var pageEntities = JdkFlowAdapter
            .flowPublisherToFlux(feedConsumer.streamPages(url3, StartFrom.beginning()))
            .flatMap(page -> JdkFlowAdapter.flowPublisherToFlux(page.toCompleteEntities()))
            .collectList()
            .single()
            .block();
        preparePageToStream();
        var entities = JdkFlowAdapter
            .flowPublisherToFlux(feedConsumer.streamEntities(url3, StartFrom.beginning()))
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
            defaultPagesHeaders1,
            "boundary-1",
            TestEntityParts.of(defaultEntityHeaders1, "first entity")
        );
        var page2 = testStreamingPageOf(
            defaultPagesHeaders2,
            "boundary-1",
            TestEntityParts.of(defaultEntityHeaders2, "second entity")
        );
        var page3 = testStreamingPageOf(
            defaultPagesHeaders3,
            "boundary-1",
            TestEntityParts.of(defaultEntityHeaders3, "third entity")
        );
        when(pageLoader.load(url1)).thenReturn(Mono.just(page1));
        when(pageLoader.load(url2)).thenReturn(Mono.just(page2));
        when(pageLoader.load(url3)).thenReturn(Mono.just(page3));
    }

    @Test
    void crawlPages_shouldThrowHttpException_fromUnderlyingHttpClient() {
        HttpException.NetworkError expectedNetworkError = new HttpException.NetworkError(url1, new IOException());
        when(feedPageCrawler.crawl(url1, StartFrom.beginning())).thenReturn(Mono.error(expectedNetworkError));

        final var result = JdkFlowAdapter
            .flowPublisherToFlux(feedConsumer.streamPages(url1, StartFrom.beginning()));

        StepVerifier
            .create(result)
            .expectNextCount(0)
            .expectErrorMatches(expectedNetworkError::equals)
            .verify();
    }

    @Test
    void loadPage_shouldThrowHttpException_fromUnderlyingHttpClient() {
        HttpException.NetworkError expectedNetworkError = new HttpException.NetworkError(url1, new IOException());
        when(feedPageCrawler.crawl(url1, StartFrom.beginning())).thenReturn(Mono.just(url1));
        when(pageLoader.load(url1)).thenReturn(Mono.error(expectedNetworkError));

        final var result = JdkFlowAdapter
            .flowPublisherToFlux(feedConsumer.streamPages(url1, StartFrom.beginning()));

        StepVerifier
            .create(result)
            .expectNextCount(0)
            .expectErrorMatches(expectedNetworkError::equals)
            .verify();
    }

    @Test
    void streamEntitiesFromTimestamp_shouldOnlyConsumeNewerEntities() {
        StartFrom startFrom = StartFrom.timestamp(lastModified);
        FeedPageHeader feedPageHeader1 = new FeedPageHeader(
            lastModified,
            Link.self(url1),
            Optional.empty(),
            Optional.empty()
        );
        var page1 = testStreamingPageOf(
            defaultPagesHeaders1,
            "boundary-1",
            TestEntityParts.of(defaultEntityHeaders1, "already consumed older timestamp"),
            TestEntityParts.of(defaultEntityHeaders2, "new entity"),
            TestEntityParts.of(defaultEntityHeaders3, "newer entity")
        );
        FeedEntityHeader feedEntityHeader = new FeedEntityHeader(lastModifiedAfter, OperationType.PUT, contentId3);

        when(feedPageHeaderParser.feedPageHeader(defaultPagesHeaders1)).thenReturn(feedPageHeader1);
        when(feedPageHeaderParser.feedEntityHeader(0, defaultEntityHeaders1))
            .thenReturn(new FeedEntityHeader(lastModifiedBefore, OperationType.PUT, contentId1));
        when(feedPageHeaderParser.feedEntityHeader(1, defaultEntityHeaders2))
            .thenReturn(new FeedEntityHeader(lastModified, OperationType.PUT, contentId2));
        when(feedPageHeaderParser.feedEntityHeader(2, defaultEntityHeaders3))
            .thenReturn(feedEntityHeader);
        when(feedPageCrawler.crawl(url1, startFrom)).thenReturn(Mono.just(url1));
        when(pageLoader.load(url1)).thenReturn(Mono.just(page1));

        List<@NonNull Entity<@NonNull FeedEntityHeader>> entities = JdkFlowAdapter
            .flowPublisherToFlux(feedConsumer.streamEntities(url1, startFrom))
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
        StartFrom startFrom = StartFrom.contentId(contentId2, lastModified);
        FeedPageHeader feedPageHeader1 = new FeedPageHeader(
            lastModified,
            Link.self(url1),
            Optional.empty(),
            Optional.empty()
        );
        var page1 = testStreamingPageOf(
            defaultPagesHeaders1,
            "boundary-1",
            TestEntityParts.of(defaultEntityHeaders1, "already consumed older timestamp"),
            TestEntityParts.of(defaultEntityHeaders2, "already consumed ContentId"),
            TestEntityParts.of(defaultEntityHeaders3, "new entity")
        );
        FeedEntityHeader feedEntityHeader = new FeedEntityHeader(lastModifiedAfter, OperationType.PUT, contentId3);

        when(feedPageHeaderParser.feedPageHeader(defaultPagesHeaders1)).thenReturn(feedPageHeader1);
        when(feedPageHeaderParser.feedEntityHeader(0, defaultEntityHeaders1))
            .thenReturn(new FeedEntityHeader(lastModifiedBefore, OperationType.PUT, contentId1));
        when(feedPageHeaderParser.feedEntityHeader(1, defaultEntityHeaders2))
            .thenReturn(new FeedEntityHeader(lastModified, OperationType.PUT, contentId2));
        when(feedPageHeaderParser.feedEntityHeader(2, defaultEntityHeaders3))
            .thenReturn(feedEntityHeader);
        when(feedPageCrawler.crawl(url1, startFrom)).thenReturn(Mono.just(url1));
        when(pageLoader.load(url1)).thenReturn(Mono.just(page1));

        List<@NonNull Entity<@NonNull FeedEntityHeader>> entities = JdkFlowAdapter
            .flowPublisherToFlux(feedConsumer.streamEntities(url1, startFrom))
            .collectList()
            .single()
            .block();

        assertThat(entities)
            .hasSize(1)
            .extracting(entity -> entity.body().toUtf8())
            .containsExactly("new entity");
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
