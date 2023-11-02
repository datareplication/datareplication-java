package io.datareplication.internal.page;

import io.datareplication.consumer.StreamingPage;
import io.datareplication.consumer.TestStreamingPage;
import io.datareplication.model.ContentType;
import io.datareplication.model.HttpHeader;
import io.datareplication.model.HttpHeaders;
import io.datareplication.model.snapshot.SnapshotEntityHeader;
import io.datareplication.model.snapshot.SnapshotPageHeader;
import org.junit.jupiter.api.Test;
import reactor.adapter.JdkFlowAdapter;
import reactor.test.StepVerifier;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

class WrappedStreamingPageTest {
    private static final HttpHeaders HEADERS_1 = HttpHeaders.of(HttpHeader.of("h1", "v1"));
    private static final HttpHeaders HEADERS_2 = HttpHeaders.of(HttpHeader.of("h2", "v2"));
    private static final HttpHeaders HEADERS_3 = HttpHeaders.of(HttpHeader.of("h3", "v3"));
    private static final ContentType CONTENT_TYPE_1 = ContentType.of("audio/ogg");
    private static final ContentType CONTENT_TYPE_2 = ContentType.of("audio/flac");
    private static final ContentType CONTENT_TYPE_3 = ContentType.of("audio/mp3");

    @Test
    void shouldReturnUnderlyingPageHeader() {
        final var underlying = new TestStreamingPage<HttpHeaders, HttpHeaders>(
            HttpHeaders.EMPTY,
            "underlying-boundary",
            Collections.emptyList());

        final var wrappedPage = new WrappedStreamingPage<>(
            underlying,
            new SnapshotPageHeader(),
            Function.identity());

        assertThat(wrappedPage.header()).isEqualTo(new SnapshotPageHeader());
        assertThat(wrappedPage.boundary()).isEqualTo("underlying-boundary");
    }

    @Test
    void shouldConvertEntityHeadersInStream() throws InterruptedException {
        final var underlying = new TestStreamingPage<>(
            HttpHeaders.EMPTY,
            "underlying-boundary",
            List.of(
                StreamingPage.Chunk.header(HEADERS_1, CONTENT_TYPE_1),
                StreamingPage.Chunk.bodyChunk(ByteBuffer.wrap(new byte[]{1, 2, 3})),
                StreamingPage.Chunk.bodyEnd(),
                StreamingPage.Chunk.header(HEADERS_2, CONTENT_TYPE_2),
                StreamingPage.Chunk.bodyEnd(),
                StreamingPage.Chunk.header(HEADERS_3, CONTENT_TYPE_3),
                StreamingPage.Chunk.bodyChunk(ByteBuffer.wrap(new byte[]{1})),
                StreamingPage.Chunk.bodyEnd()
            ));

        final var wrappedPage = new WrappedStreamingPage<>(
            underlying,
            new SnapshotPageHeader(),
            SnapshotEntityHeader::new);

        assertThat(JdkFlowAdapter.flowPublisherToFlux(wrappedPage).collectList().block())
            .containsExactly(
                StreamingPage.Chunk.header(new SnapshotEntityHeader(HEADERS_1), CONTENT_TYPE_1),
                StreamingPage.Chunk.bodyChunk(ByteBuffer.wrap(new byte[]{1, 2, 3})),
                StreamingPage.Chunk.bodyEnd(),
                StreamingPage.Chunk.header(new SnapshotEntityHeader(HEADERS_2), CONTENT_TYPE_2),
                StreamingPage.Chunk.bodyEnd(),
                StreamingPage.Chunk.header(new SnapshotEntityHeader(HEADERS_3), CONTENT_TYPE_3),
                StreamingPage.Chunk.bodyChunk(ByteBuffer.wrap(new byte[]{1})),
                StreamingPage.Chunk.bodyEnd()
            );
    }

    @Test
    void shouldPassThroughExceptionFromConvertFunction() throws InterruptedException {
        final var expectedException = new RuntimeException("test");
        final var underlying = new TestStreamingPage<>(
            HttpHeaders.EMPTY,
            "underlying-boundary",
            List.of(
                StreamingPage.Chunk.bodyChunk(ByteBuffer.wrap(new byte[]{1})),
                StreamingPage.Chunk.bodyEnd(),
                StreamingPage.Chunk.header(HEADERS_1, CONTENT_TYPE_1),
                StreamingPage.Chunk.bodyEnd()
            ));

        final var wrappedPage = new WrappedStreamingPage<>(
            underlying,
            new SnapshotPageHeader(),
            header -> {
                throw expectedException;
            });

        StepVerifier
            .create(JdkFlowAdapter.flowPublisherToFlux(wrappedPage))
            .expectNext(
                StreamingPage.Chunk.bodyChunk(ByteBuffer.wrap(new byte[]{1})),
                StreamingPage.Chunk.bodyEnd()
            )
            .expectErrorMatches(expectedException::equals)
            .verify();
    }
}
