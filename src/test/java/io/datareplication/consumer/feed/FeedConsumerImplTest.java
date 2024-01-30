package io.datareplication.consumer.feed;

import io.datareplication.consumer.StreamingPage;
import io.datareplication.consumer.TestStreamingPage;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.adapter.JdkFlowAdapter;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FeedConsumerImplTest {
    @Mock
    private FeedPageCrawler feedPageCrawler;
    @Mock
    private PageLoader pageLoader;
    @Mock
    private FeedPageHeaderParser feedPageHeaderParser;
    @InjectMocks
    private FeedConsumerImpl feedConsumer;
    private HttpHeaders defaultEntityHeaders;
    private HttpHeaders defaultPagesHeaders;

    @BeforeEach
    void setUp() {
        defaultEntityHeaders = HttpHeaders.of(HttpHeader.of("entity", "first"));
        defaultPagesHeaders = HttpHeaders.of(HttpHeader.of("page", "this"));
    }

    @Test
    void loadLatestSite_shouldConsumeOneEntry() {
        Timestamp lastModified = Timestamp.fromRfc1123String("Thu, 5 Oct 2023 03:00:14 GMT");
        Url url = Url.of("dummy url");
        ContentId contentId = ContentId.of("any ID");
        FeedEntityHeader feedEntityHeader = new FeedEntityHeader(lastModified, OperationType.PUT, contentId);
        FeedPageHeader feedPageHeader = new FeedPageHeader(lastModified, Link.self(url), Optional.empty(), Optional.empty());
        when(feedPageHeaderParser.feedPageHeader(defaultPagesHeaders))
            .thenReturn(feedPageHeader);
        when(feedPageHeaderParser.feedEntityHeader(0, defaultEntityHeaders))
            .thenReturn(feedEntityHeader);
        when(feedPageCrawler.crawl(url, StartFrom.beginning()))
            .thenReturn(CompletableFuture.supplyAsync(() -> feedPageHeader));
        when(pageLoader.load(url))
            .thenReturn(Mono.just(
                new TestStreamingPage<>(defaultPagesHeaders,
                    "boundary-1",
                    List.of(
                        StreamingPage.Chunk.header(defaultEntityHeaders, ContentType.of("text/plain")),
                        StreamingPage.Chunk.bodyChunk(utf8("Hello World!")),
                        StreamingPage.Chunk.bodyEnd()
                    ))
            ));

        List<@NonNull StreamingPage<@NonNull FeedPageHeader, @NonNull FeedEntityHeader>> pages = JdkFlowAdapter
            .flowPublisherToFlux(feedConsumer.streamPages(url, StartFrom.beginning()))
            .collectList()
            .single()
            .block();

        assertThat(pages).hasSize(1);
        assertThat(
            Flux
                .fromIterable(pages)
                .flatMap(page -> JdkFlowAdapter.flowPublisherToFlux(page.toCompleteEntities()))
                .collectList()
                .single()
                .block()
        )
            .usingRecursiveFieldByFieldElementComparator(BodyTestUtil.bodyContentsComparator())
            .containsExactly(new Entity<>(feedEntityHeader, Body.fromUtf8("Hello World!", ContentType.of("text/plain"))));
    }

    private static ByteBuffer utf8(String s) {
        return ByteBuffer.wrap(s.getBytes(StandardCharsets.UTF_8));
    }
}
