package io.datareplication.consumer.snapshot;

import io.datareplication.consumer.ConsumerException;
import io.datareplication.consumer.HttpException;
import io.datareplication.consumer.StreamingPage;
import io.datareplication.consumer.TestStreamingPage;
import io.datareplication.internal.http.HttpClient;
import io.datareplication.internal.http.TestHttpResponse;
import io.datareplication.internal.page.PageLoader;
import io.datareplication.model.Body;
import io.datareplication.model.BodyTestUtil;
import io.datareplication.model.ContentType;
import io.datareplication.model.Entity;
import io.datareplication.model.HttpHeader;
import io.datareplication.model.HttpHeaders;
import io.datareplication.model.Url;
import io.datareplication.model.snapshot.SnapshotEntityHeader;
import io.datareplication.model.snapshot.SnapshotId;
import io.datareplication.model.snapshot.SnapshotIndex;
import io.datareplication.model.snapshot.SnapshotPageHeader;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.adapter.JdkFlowAdapter;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SnapshotConsumerImplTest {
    @Mock
    private HttpClient httpClient;
    @Mock
    private PageLoader pageLoader;
    private SnapshotConsumerImpl snapshotConsumer;

    @BeforeEach
    void setup() {
        snapshotConsumer = new SnapshotConsumerImpl(httpClient,
                                                    pageLoader,
                                                    1,
                                                    false);
    }

    private static final Url SOME_URL = Url.of("https://example.datareplication.io/snapshotindex.json");

    private static ByteBuffer utf8(String s) {
        return ByteBuffer.wrap(s.getBytes(StandardCharsets.UTF_8));
    }

    private static String resourceAsString(String name) {
        try (var stream = SnapshotConsumerImpl.class.getClassLoader().getResourceAsStream(name)) {
            return IOUtils.toString(Objects.requireNonNull(stream), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void loadSnapshotIndex_shouldLoadSnapshotIndex() throws InterruptedException, ExecutionException {
        final var snapshotIndexJson = resourceAsString("__files/snapshot/index.json");
        final var expectedSnapshotIndex = new SnapshotIndex(
            SnapshotId.of("example"),
            Instant.parse("2023-10-07T15:00:00.000Z"),
            List.of(
                Url.of("http://localhost:8443/1.content.multipart"),
                Url.of("http://localhost:8443/2.content.multipart"),
                Url.of("http://localhost:8443/3.content.multipart")
            )
        );
        when(httpClient.get(eq(SOME_URL), any())).thenReturn(
            Mono.just(new TestHttpResponse<>(snapshotIndexJson.getBytes(StandardCharsets.UTF_8)))
        );

        final var result = snapshotConsumer.loadSnapshotIndex(SOME_URL)
            .toCompletableFuture()
            .get();

        assertThat(result).isEqualTo(expectedSnapshotIndex);
    }

    @Test
    void loadSnapshotIndex_shouldThrowParsingException_whenInvalidJson() {
        when(httpClient.get(eq(SOME_URL), any())).thenReturn(
            Mono.just(new TestHttpResponse<>("{\"key\":4".getBytes(StandardCharsets.UTF_8)))
        );

        final var result = Mono.fromCompletionStage(snapshotConsumer.loadSnapshotIndex(SOME_URL));

        StepVerifier
            .create(result)
            .expectNextCount(0)
            .expectError(SnapshotIndex.ParsingException.class)
            .verify();
    }

    @Test
    void loadSnapshotIndex_shouldThrowHttpException_fromUnderlyingHttpClient() {
        when(httpClient.get(eq(SOME_URL), any())).thenReturn(
            Mono.error(new HttpException.ClientError(SOME_URL, 404))
        );

        final var result = Mono.fromCompletionStage(snapshotConsumer.loadSnapshotIndex(SOME_URL));

        StepVerifier
            .create(result)
            .expectNextCount(0)
            .expectErrorMatches(new HttpException.ClientError(SOME_URL, 404)::equals)
            .verify();
    }

    @Test
    void streamPages_shouldLoadAllPages() {
        final var url1 = Url.of("https://example.datareplication.io/snapshotpage/1");
        final var url2 = Url.of("https://example.datareplication.io/snapshotpage/2");
        final var url3 = Url.of("https://example.datareplication.io/snapshotpage/3");
        final var headers1 = HttpHeaders.of(HttpHeader.of("h1", "v1"));
        final var headers2 = HttpHeaders.of(HttpHeader.of("h2", "v2"),
                                            HttpHeader.of("h3", "v3"));
        final var headers3 = HttpHeaders.of(HttpHeader.of("h4", "v4"));
        when(pageLoader.load(url1)).thenReturn(Mono.just(
            new TestStreamingPage<>(headers1,
                                    "boundary-1",
                                    Collections.emptyList())
        ));
        when(pageLoader.load(url2)).thenReturn(Mono.just(
            new TestStreamingPage<>(headers2,
                                    "boundary-2",
                                    List.of(
                                        StreamingPage.Chunk.header(headers3, ContentType.of("text/plain")),
                                        StreamingPage.Chunk.bodyChunk(utf8("test")),
                                        StreamingPage.Chunk.bodyEnd()
                                    ))
        ));
        when(pageLoader.load(url3)).thenReturn(Mono.just(
            new TestStreamingPage<>(HttpHeaders.EMPTY,
                                    "boundary-3",
                                    Collections.emptyList())
        ));
        final var snapshotIndex = new SnapshotIndex(
            SnapshotId.of("doesn't matter"),
            Instant.now(),
            List.of(url1, url2, url3));

        final var pages = JdkFlowAdapter
            .flowPublisherToFlux(snapshotConsumer.streamPages(snapshotIndex))
            .collectList()
            .single()
            .block();

        assertThat(pages).hasSize(3);
        assertThat(pages.get(0).header()).isEqualTo(new SnapshotPageHeader(headers1));
        assertThat(pages.get(0).boundary()).isEqualTo("boundary-1");
        assertThat(JdkFlowAdapter.flowPublisherToFlux(pages.get(0)).collectList().block())
            .isEmpty();
        assertThat(pages.get(1).header()).isEqualTo(new SnapshotPageHeader(headers2));
        assertThat(pages.get(1).boundary()).isEqualTo("boundary-2");
        assertThat(JdkFlowAdapter.flowPublisherToFlux(pages.get(1)).collectList().block())
            .containsExactly(
                StreamingPage.Chunk.header(new SnapshotEntityHeader(headers3), ContentType.of("text/plain")),
                StreamingPage.Chunk.bodyChunk(utf8("test")),
                StreamingPage.Chunk.bodyEnd()
            );
        assertThat(pages.get(2).header()).isEqualTo(new SnapshotPageHeader(HttpHeaders.EMPTY));
        assertThat(pages.get(2).boundary()).isEqualTo("boundary-3");
        assertThat(JdkFlowAdapter.flowPublisherToFlux(pages.get(2)).collectList().block())
            .isEmpty();
    }

    @Test
    void streamPages_shouldPassThroughExceptionsFromPageLoader() {
        final var expectedException = new HttpException.NetworkError(SOME_URL, new IOException("oops"));
        final var url1 = Url.of("https://example.datareplication.io/snapshotpage/1");
        when(pageLoader.load(url1)).thenReturn(Mono.error(expectedException));
        final var snapshotIndex = new SnapshotIndex(
            SnapshotId.of("doesn't matter"),
            Instant.now(),
            List.of(url1));

        StepVerifier
            .create(JdkFlowAdapter.flowPublisherToFlux(snapshotConsumer.streamPages(snapshotIndex)))
            .expectNextCount(0)
            .expectErrorMatches(expectedException::equals)
            .verify();
    }

    @Test
    void streamEntities_shouldLoadAllPagesAndStreamAllEntities() {
        final Url url1 = Url.of("https://example.datareplication.io/snapshotpage/1");
        final Url url2 = Url.of("https://example.datareplication.io/snapshotpage/2");
        final HttpHeaders headers1 = HttpHeaders.of(HttpHeader.of("h1", "v1"));
        final HttpHeaders headers2 = HttpHeaders.of(HttpHeader.of("h2", "v2"));
        final HttpHeaders headers3 = HttpHeaders.of(HttpHeader.of("h3", "v3"));
        when(pageLoader.load(url1)).thenReturn(Mono.just(
            new TestStreamingPage<>(HttpHeaders.EMPTY,
                                    "",
                                    List.of(
                                        StreamingPage.Chunk.header(headers1, ContentType.of("text/plain")),
                                        StreamingPage.Chunk.bodyChunk(utf8("ab")),
                                        StreamingPage.Chunk.bodyChunk(utf8("cd")),
                                        StreamingPage.Chunk.bodyChunk(utf8("ef")),
                                        StreamingPage.Chunk.bodyEnd(),
                                        StreamingPage.Chunk.header(headers2, ContentType.of("audio/mp3")),
                                        StreamingPage.Chunk.bodyChunk(utf8("12")),
                                        StreamingPage.Chunk.bodyChunk(utf8("34")),
                                        StreamingPage.Chunk.bodyEnd()
                                    ))
        ));
        when(pageLoader.load(url2)).thenReturn(Mono.just(
            new TestStreamingPage<>(HttpHeaders.EMPTY,
                                    "",
                                    List.of(
                                        StreamingPage.Chunk.header(headers3, ContentType.of("audio/ogg")),
                                        StreamingPage.Chunk.bodyChunk(utf8("testtesttest")),
                                        StreamingPage.Chunk.bodyEnd()
                                    ))
        ));
        final SnapshotIndex snapshotIndex = new SnapshotIndex(
            SnapshotId.of("doesn't matter"),
            Instant.now(),
            List.of(url1, url2));

        final var entities = JdkFlowAdapter
            .flowPublisherToFlux(snapshotConsumer.streamEntities(snapshotIndex))
            .collectList()
            .single()
            .block();

        assertThat(entities)
            .usingRecursiveFieldByFieldElementComparator(BodyTestUtil.bodyContentsComparator())
            .containsExactly(
                new Entity<>(new SnapshotEntityHeader(headers1),
                             Body.fromUtf8("abcdef", ContentType.of("text/plain"))),
                new Entity<>(new SnapshotEntityHeader(headers2),
                             Body.fromUtf8("1234", ContentType.of("audio/mp3"))),
                new Entity<>(new SnapshotEntityHeader(headers3),
                             Body.fromUtf8("testtesttest", ContentType.of("audio/ogg")))
            );
    }

    @Test
    void streamEntities_shouldLoadAllPagesAndStreamAllEntitiesConcurrently() {
        snapshotConsumer = new SnapshotConsumerImpl(httpClient,
                                                    pageLoader,
                                                    10,
                                                    false);

        final Url url1 = Url.of("https://example.datareplication.io/snapshotpage/1");
        final Url url2 = Url.of("https://example.datareplication.io/snapshotpage/2");
        final Url url3 = Url.of("https://example.datareplication.io/snapshotpage/3");
        final Url url4 = Url.of("https://example.datareplication.io/snapshotpage/4");
        final Url url5 = Url.of("https://example.datareplication.io/snapshotpage/5");
        final HttpHeaders headers1 = HttpHeaders.of(HttpHeader.of("h1", "v1"));
        when(pageLoader.load(url1)).thenReturn(Mono.just(
            new TestStreamingPage<>(HttpHeaders.EMPTY,
                                    "",
                                    Collections.emptyList())
        ));
        when(pageLoader.load(url2)).thenReturn(Mono.just(
            new TestStreamingPage<>(HttpHeaders.EMPTY,
                                    "",
                                    Collections.emptyList())
        ));
        when(pageLoader.load(url3)).thenReturn(Mono.just(
            new TestStreamingPage<>(HttpHeaders.EMPTY,
                                    "",
                                    Collections.emptyList())
        ));
        when(pageLoader.load(url4)).thenReturn(Mono.just(
            new TestStreamingPage<>(HttpHeaders.EMPTY,
                                    "",
                                    Collections.emptyList())
        ));
        when(pageLoader.load(url5)).thenReturn(Mono.just(
            new TestStreamingPage<>(HttpHeaders.EMPTY,
                                    "",
                                    List.of(
                                        StreamingPage.Chunk.header(headers1, ContentType.of("audio/ogg")),
                                        StreamingPage.Chunk.bodyChunk(utf8("testtesttest")),
                                        StreamingPage.Chunk.bodyEnd()
                                    ))
        ));
        final SnapshotIndex snapshotIndex = new SnapshotIndex(
            SnapshotId.of("doesn't matter"),
            Instant.now(),
            List.of(url1, url2, url3, url4, url5));

        final var entities = JdkFlowAdapter
            .flowPublisherToFlux(snapshotConsumer.streamEntities(snapshotIndex))
            .collectList()
            .single()
            .block();

        assertThat(entities)
            .usingRecursiveFieldByFieldElementComparator(BodyTestUtil.bodyContentsComparator())
            .containsExactly(
                new Entity<>(new SnapshotEntityHeader(headers1),
                             Body.fromUtf8("testtesttest", ContentType.of("audio/ogg")))
            );
    }

    @Test
    void streamEntities_shouldPassThroughExceptionsFromPageLoader() {
        final var expectedException = new HttpException.NetworkError(SOME_URL, new IOException("oops"));
        final var url1 = Url.of("https://example.datareplication.io/snapshotpage/1");
        when(pageLoader.load(url1)).thenReturn(Mono.error(expectedException));
        final var snapshotIndex = new SnapshotIndex(
            SnapshotId.of("doesn't matter"),
            Instant.now(),
            List.of(url1));

        StepVerifier
            .create(JdkFlowAdapter.flowPublisherToFlux(snapshotConsumer.streamEntities(snapshotIndex)))
            .expectNextCount(0)
            .expectErrorMatches(expectedException::equals)
            .verify();
    }

    @Test
    void streamEntities_shouldPassThroughExceptionsFromPage() {
        final var expectedException = new HttpException.NetworkError(SOME_URL, new IOException("oops"));
        final var url1 = Url.of("https://example.datareplication.io/snapshotpage/1");
        when(pageLoader.load(url1)).thenReturn(Mono.just(
            new TestStreamingPage<>(
                HttpHeaders.EMPTY,
                "",
                Flux
                    .<StreamingPage.Chunk<HttpHeaders>>fromIterable(List.of(
                        StreamingPage.Chunk.header(HttpHeaders.EMPTY, ContentType.of("text/plain")),
                        StreamingPage.Chunk.bodyEnd()
                    ))
                    .concatWith(Mono.error(expectedException))
            )
        ));
        final var snapshotIndex = new SnapshotIndex(
            SnapshotId.of("doesn't matter"),
            Instant.now(),
            List.of(url1));

        StepVerifier
            .create(JdkFlowAdapter.flowPublisherToFlux(snapshotConsumer.streamEntities(snapshotIndex)))
            .expectNextCount(1)
            .expectErrorMatches(expectedException::equals)
            .verify();
    }

    @Test
    void streamPages_shouldDelayExceptionsFromPageLoader() {
        snapshotConsumer = new SnapshotConsumerImpl(httpClient,
                                                    pageLoader,
                                                    1,
                                                    true);

        final var url1 = Url.of("https://example.datareplication.io/snapshotpage/1");
        final var url2 = Url.of("https://example.datareplication.io/snapshotpage/2");
        final var url3 = Url.of("https://example.datareplication.io/snapshotpage/3");
        final var exc1 = new HttpException.NetworkError(url1, new IOException("haha"));
        final var exc2 = new HttpException.NetworkError(url2, new IOException("hehe"));
        final var exc3 = new HttpException.NetworkError(url3, new IOException("hoho"));
        final var goodUrl = Url.of("https://example.datareplication.io/snapshotpage/good");
        when(pageLoader.load(url1)).thenReturn(Mono.error(exc1));
        when(pageLoader.load(url2)).thenReturn(Mono.error(exc2));
        when(pageLoader.load(url3)).thenReturn(Mono.error(exc3));
        when(pageLoader.load(goodUrl)).thenReturn(Mono.just(
            new TestStreamingPage<>(HttpHeaders.EMPTY,
                                    "good-page-boundary",
                                    Collections.emptyList())
        ));
        final var snapshotIndex = new SnapshotIndex(
            SnapshotId.of("doesn't matter"),
            Instant.now(),
            List.of(goodUrl, url1, goodUrl, goodUrl, url2, url3, goodUrl));

        StepVerifier
            .create(JdkFlowAdapter.flowPublisherToFlux(snapshotConsumer.streamPages(snapshotIndex)))
            .expectNextCount(4)
            .expectErrorMatches(new ConsumerException.CollectedErrors(List.of(exc1, exc2, exc3))::equals)
            .verify();
    }

    @Test
    void streamEntities_shouldDelayExceptionsFromPage() {
        snapshotConsumer = new SnapshotConsumerImpl(httpClient,
                                                    pageLoader,
                                                    1,
                                                    true);

        final var url1 = Url.of("https://example.datareplication.io/snapshotpage/1");
        final var url2 = Url.of("https://example.datareplication.io/snapshotpage/2");
        final var url3 = Url.of("https://example.datareplication.io/snapshotpage/3");
        final var exc1 = new HttpException.NetworkError(url1, new IOException("oops"));
        final var exc3 = new HttpException.NetworkError(url3, new IOException("whoops"));
        when(pageLoader.load(url1)).thenReturn(Mono.just(
            new TestStreamingPage<>(HttpHeaders.EMPTY,
                                    "",
                                    Flux.error(exc1))
        ));
        when(pageLoader.load(url2)).thenReturn(Mono.just(
            new TestStreamingPage<>(HttpHeaders.EMPTY,
                                    "",
                                    List.of(
                                        StreamingPage.Chunk.header(HttpHeaders.EMPTY, ContentType.of("")),
                                        StreamingPage.Chunk.bodyEnd()
                                    ))
        ));
        when(pageLoader.load(url3)).thenReturn(Mono.just(
            new TestStreamingPage<>(HttpHeaders.EMPTY,
                                    "",
                                    Flux.<StreamingPage.Chunk<HttpHeaders>>fromIterable(List.of(
                                            StreamingPage.Chunk.header(HttpHeaders.EMPTY, ContentType.of("")),
                                            StreamingPage.Chunk.bodyEnd()))
                                        .concatWith(Mono.error(exc3)))
        ));
        final SnapshotIndex snapshotIndex = new SnapshotIndex(
            SnapshotId.of("doesn't matter"),
            Instant.now(),
            List.of(url1, url2, url3));

        StepVerifier
            .create(JdkFlowAdapter.flowPublisherToFlux(snapshotConsumer.streamEntities(snapshotIndex)))
            .expectNextCount(2)
            .expectErrorMatches(new ConsumerException.CollectedErrors(List.of(exc1, exc3))::equals)
            .verify();
    }
}
