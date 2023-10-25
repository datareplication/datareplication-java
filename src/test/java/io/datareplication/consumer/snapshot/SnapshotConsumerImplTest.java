package io.datareplication.consumer.snapshot;

import io.datareplication.consumer.HttpException;
import io.datareplication.consumer.StreamingPage;
import io.datareplication.consumer.TestStreamingPage;
import io.datareplication.internal.http.HttpClient;
import io.datareplication.internal.http.TestHttpResponse;
import io.datareplication.internal.page.PageLoader;
import io.datareplication.model.Body;
import io.datareplication.model.ContentType;
import io.datareplication.model.Entity;
import io.datareplication.model.HttpHeader;
import io.datareplication.model.HttpHeaders;
import io.datareplication.model.Timestamp;
import io.datareplication.model.Url;
import io.datareplication.model.snapshot.SnapshotEntityHeader;
import io.datareplication.model.snapshot.SnapshotId;
import io.datareplication.model.snapshot.SnapshotIndex;
import io.datareplication.model.snapshot.SnapshotPageHeader;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import lombok.NonNull;
import org.apache.commons.io.IOUtils;
import org.assertj.core.api.recursive.comparison.RecursiveComparisonConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.reactivestreams.FlowAdapters;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

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
        snapshotConsumer = new SnapshotConsumerImpl(httpClient, pageLoader, 1);
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

    private static boolean compareBodies(final Body a, final Body b) {
        try (var in1 = a.newInputStream()) {
            try (var in2 = b.newInputStream()) {
                return IOUtils.contentEquals(in1, in2)
                    && a.contentLength() == b.contentLength()
                    && a.contentType().equals(b.contentType());
            }
        } catch (IOException e) {
            throw new IllegalStateException("IOException when comparing Body", e);
        }
    }

    @Test
    void loadSnapshotIndex_shouldLoadSnapshotIndex() throws InterruptedException {
        final var snapshotIndexJson = resourceAsString("__files/snapshot/index.json");
        final var expectedSnapshotIndex = new SnapshotIndex(
            SnapshotId.of("example"),
            Timestamp.of(Instant.parse("2023-10-07T15:00:00.000Z")),
            List.of(
                Url.of("http://localhost:8443/1.content.multipart"),
                Url.of("http://localhost:8443/2.content.multipart"),
                Url.of("http://localhost:8443/3.content.multipart")
            )
        );
        when(httpClient.get(eq(SOME_URL), any())).thenReturn(
            Single.just(new TestHttpResponse<>(snapshotIndexJson.getBytes(StandardCharsets.UTF_8)))
        );

        Single
            .fromCompletionStage(snapshotConsumer.loadSnapshotIndex(SOME_URL))
            .test()
            .await()
            .assertValue(expectedSnapshotIndex);
    }

    @Test
    void loadSnapshotIndex_shouldThrowParsingException_whenInvalidJson() throws InterruptedException {
        when(httpClient.get(eq(SOME_URL), any())).thenReturn(
            Single.just(new TestHttpResponse<>("{\"key\":4".getBytes(StandardCharsets.UTF_8)))
        );

        Single
            .fromCompletionStage(snapshotConsumer.loadSnapshotIndex(SOME_URL))
            .test()
            .await()
            .assertNoValues()
            .assertError(SnapshotIndex.ParsingException.class);
    }

    @Test
    void loadSnapshotIndex_shouldThrowHttpException_fromUnderlyingHttpClient() throws InterruptedException {
        when(httpClient.get(eq(SOME_URL), any())).thenReturn(
            Single.error(new HttpException.ClientError(SOME_URL, 404))
        );

        Single
            .fromCompletionStage(snapshotConsumer.loadSnapshotIndex(SOME_URL))
            .test()
            .await()
            .assertNoValues()
            .assertError(new HttpException.ClientError(SOME_URL, 404));
    }

    @Test
    void streamPages_shouldLoadAllPages() throws InterruptedException {
        final var url1 = Url.of("https://example.datareplication.io/snapshotpage/1");
        final var url2 = Url.of("https://example.datareplication.io/snapshotpage/2");
        final var url3 = Url.of("https://example.datareplication.io/snapshotpage/3");
        final var headers1 = HttpHeaders.of(HttpHeader.of("h1", "v1"));
        final var headers2 = HttpHeaders.of(HttpHeader.of("h2", "v2"),
                                            HttpHeader.of("h3", "v3"));
        final var headers3 = HttpHeaders.of(HttpHeader.of("h4", "v4"));
        when(pageLoader.load(url1)).thenReturn(Single.just(
            new TestStreamingPage<>(headers1,
                                    "boundary-1",
                                    Collections.emptyList())
        ));
        when(pageLoader.load(url2)).thenReturn(Single.just(
            new TestStreamingPage<>(headers2,
                                    "boundary-2",
                                    List.of(
                                        StreamingPage.Chunk.header(headers3, ContentType.of("text/plain")),
                                        StreamingPage.Chunk.bodyChunk(utf8("test")),
                                        StreamingPage.Chunk.bodyEnd()
                                    ))
        ));
        when(pageLoader.load(url3)).thenReturn(Single.just(
            new TestStreamingPage<>(HttpHeaders.EMPTY,
                                    "boundary-3",
                                    Collections.emptyList())
        ));
        final var snapshotIndex = new SnapshotIndex(
            SnapshotId.of("doesn't matter"),
            Timestamp.now(),
            List.of(url1, url2, url3));

        final var pages = Flowable
            .fromPublisher(FlowAdapters.toPublisher(snapshotConsumer.streamPages(snapshotIndex)))
            .toList()
            .blockingGet();

        assertThat(pages).hasSize(3);
        assertThat(pages.get(0).header()).isEqualTo(new SnapshotPageHeader(headers1));
        assertThat(pages.get(0).boundary()).isEqualTo("boundary-1");
        Flowable
            .fromPublisher(FlowAdapters.toPublisher(pages.get(0)))
            .test()
            .await()
            .assertNoValues();
        assertThat(pages.get(1).header()).isEqualTo(new SnapshotPageHeader(headers2));
        assertThat(pages.get(1).boundary()).isEqualTo("boundary-2");
        Flowable
            .fromPublisher(FlowAdapters.toPublisher(pages.get(1)))
            .test()
            .await()
            .assertValues(
                StreamingPage.Chunk.header(new SnapshotEntityHeader(headers3), ContentType.of("text/plain")),
                StreamingPage.Chunk.bodyChunk(utf8("test")),
                StreamingPage.Chunk.bodyEnd()
            );
        assertThat(pages.get(2).header()).isEqualTo(new SnapshotPageHeader(HttpHeaders.EMPTY));
        assertThat(pages.get(2).boundary()).isEqualTo("boundary-3");
        Flowable
            .fromPublisher(FlowAdapters.toPublisher(pages.get(2)))
            .test()
            .await()
            .assertNoValues();
    }

    @Test
    void streamPages_shouldPassThroughExceptionsFromPageLoader() throws InterruptedException {
        final var expectedException = new HttpException.NetworkError(SOME_URL, new IOException("oops"));
        final var url1 = Url.of("https://example.datareplication.io/snapshotpage/1");
        when(pageLoader.load(url1)).thenReturn(Single.error(expectedException));
        final var snapshotIndex = new SnapshotIndex(
            SnapshotId.of("doesn't matter"),
            Timestamp.now(),
            List.of(url1));

        Flowable
            .fromPublisher(FlowAdapters.toPublisher(snapshotConsumer.streamPages(snapshotIndex)))
            .test()
            .await()
            .assertNoValues()
            .assertError(expectedException);
    }

    @Test
    void streamEntities_shouldLoadAllPagesAndStreamAllEntities() {
        final Url url1 = Url.of("https://example.datareplication.io/snapshotpage/1");
        final Url url2 = Url.of("https://example.datareplication.io/snapshotpage/2");
        final HttpHeaders headers1 = HttpHeaders.of(HttpHeader.of("h1", "v1"));
        final HttpHeaders headers2 = HttpHeaders.of(HttpHeader.of("h2", "v2"));
        final HttpHeaders headers3 = HttpHeaders.of(HttpHeader.of("h3", "v3"));
        when(pageLoader.load(url1)).thenReturn(Single.just(
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
        when(pageLoader.load(url2)).thenReturn(Single.just(
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
            Timestamp.now(),
            List.of(url1, url2));

        final List<@NonNull Entity<@NonNull SnapshotEntityHeader>> entities = Flowable
            .fromPublisher(FlowAdapters.toPublisher(snapshotConsumer.streamEntities(snapshotIndex)))
            .toList()
            .blockingGet();

        assertThat(entities)
            .usingRecursiveFieldByFieldElementComparator(
                RecursiveComparisonConfiguration
                    .builder()
                    .withEqualsForType(SnapshotConsumerImplTest::compareBodies, Body.class)
                    .build())
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
        snapshotConsumer = new SnapshotConsumerImpl(httpClient, pageLoader, 10);

        final Url url1 = Url.of("https://example.datareplication.io/snapshotpage/1");
        final Url url2 = Url.of("https://example.datareplication.io/snapshotpage/2");
        final Url url3 = Url.of("https://example.datareplication.io/snapshotpage/3");
        final Url url4 = Url.of("https://example.datareplication.io/snapshotpage/4");
        final Url url5 = Url.of("https://example.datareplication.io/snapshotpage/5");
        final HttpHeaders headers1 = HttpHeaders.of(HttpHeader.of("h1", "v1"));
        when(pageLoader.load(url1)).thenReturn(Single.just(
            new TestStreamingPage<>(HttpHeaders.EMPTY,
                                    "",
                                    Collections.emptyList())
        ));
        when(pageLoader.load(url2)).thenReturn(Single.just(
            new TestStreamingPage<>(HttpHeaders.EMPTY,
                                    "",
                                    Collections.emptyList())
        ));
        when(pageLoader.load(url3)).thenReturn(Single.just(
            new TestStreamingPage<>(HttpHeaders.EMPTY,
                                    "",
                                    Collections.emptyList())
        ));
        when(pageLoader.load(url4)).thenReturn(Single.just(
            new TestStreamingPage<>(HttpHeaders.EMPTY,
                                    "",
                                    Collections.emptyList())
        ));
        when(pageLoader.load(url5)).thenReturn(Single.just(
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
            Timestamp.now(),
            List.of(url1, url2, url3, url4, url5));

        final List<@NonNull Entity<@NonNull SnapshotEntityHeader>> entities = Flowable
            .fromPublisher(FlowAdapters.toPublisher(snapshotConsumer.streamEntities(snapshotIndex)))
            .toList()
            .blockingGet();

        assertThat(entities)
            .usingRecursiveFieldByFieldElementComparator(
                RecursiveComparisonConfiguration
                    .builder()
                    .withEqualsForType(SnapshotConsumerImplTest::compareBodies, Body.class)
                    .build())
            .containsExactly(
                new Entity<>(new SnapshotEntityHeader(headers1),
                             Body.fromUtf8("testtesttest", ContentType.of("audio/ogg")))
            );
    }

    @Test
    void streamEntities_shouldPassThroughExceptionsFromPageLoader() throws InterruptedException {
        final var expectedException = new HttpException.NetworkError(SOME_URL, new IOException("oops"));
        final var url1 = Url.of("https://example.datareplication.io/snapshotpage/1");
        when(pageLoader.load(url1)).thenReturn(Single.error(expectedException));
        final var snapshotIndex = new SnapshotIndex(
            SnapshotId.of("doesn't matter"),
            Timestamp.now(),
            List.of(url1));

        Flowable
            .fromPublisher(FlowAdapters.toPublisher(snapshotConsumer.streamEntities(snapshotIndex)))
            .test()
            .await()
            .assertNoValues()
            .assertError(expectedException);
    }

    @Test
    void streamEntities_shouldPassThroughExceptionsFromPage() throws InterruptedException {
        final var expectedException = new HttpException.NetworkError(SOME_URL, new IOException("oops"));
        final var url1 = Url.of("https://example.datareplication.io/snapshotpage/1");
        when(pageLoader.load(url1)).thenReturn(Single.just(
            new TestStreamingPage<>(
                HttpHeaders.EMPTY,
                "",
                Flowable
                    .<StreamingPage.Chunk<HttpHeaders>>fromArray(
                        StreamingPage.Chunk.header(HttpHeaders.EMPTY, ContentType.of("text/plain")),
                        StreamingPage.Chunk.bodyEnd())
                    .concatWith(Single.error(expectedException))
            )
        ));
        final var snapshotIndex = new SnapshotIndex(
            SnapshotId.of("doesn't matter"),
            Timestamp.now(),
            List.of(url1));

        Flowable
            .fromPublisher(FlowAdapters.toPublisher(snapshotConsumer.streamEntities(snapshotIndex)))
            .test()
            .await()
            .assertValueCount(1)
            .assertError(expectedException);
    }
}
