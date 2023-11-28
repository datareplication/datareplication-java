package io.datareplication.consumer;

import io.datareplication.model.Body;
import io.datareplication.model.ContentType;
import io.datareplication.model.Entity;
import io.datareplication.model.HttpHeader;
import io.datareplication.model.HttpHeaders;
import io.datareplication.model.Page;
import io.datareplication.model.Url;
import org.junit.jupiter.api.Test;
import reactor.adapter.JdkFlowAdapter;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;

class StreamingPageTest {
    private static final HttpHeaders HEADERS_1 = HttpHeaders.of(HttpHeader.of("h1", "v1"));
    private static final ContentType CONTENT_TYPE_1 = ContentType.of("text/plain");
    private static final HttpHeaders HEADERS_2 = HttpHeaders.of(HttpHeader.of("h2", "v2"));
    private static final ContentType CONTENT_TYPE_2 = ContentType.of("audio/mp3");

    private static ByteBuffer utf8(String s) {
        return ByteBuffer.wrap(s.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void toCompleteEntities_shouldStreamEntities() {
        final TestStreamingPage<HttpHeaders, HttpHeaders> streamingPage = new TestStreamingPage<>(
            HttpHeaders.EMPTY,
            "",
            List.of(StreamingPage.Chunk.header(HEADERS_1, CONTENT_TYPE_1),
                    StreamingPage.Chunk.bodyChunk(utf8("abc")),
                    StreamingPage.Chunk.bodyChunk(utf8("def")),
                    StreamingPage.Chunk.bodyEnd()));

        final var result = JdkFlowAdapter
            .flowPublisherToFlux(streamingPage.toCompleteEntities())
            .collectList()
            .single()
            .block();

        assertThat(result).containsExactly(
            new Entity<>(HEADERS_1,
                         Body.fromBytes("abcdef".getBytes(StandardCharsets.UTF_8), CONTENT_TYPE_1)));
    }

    @Test
    void toCompleteEntities_shouldPassThroughExceptionFromPage() {
        final var expectedException = new HttpException.NetworkError(Url.of(""), new IOException("whoops"));
        final var streamingPage = new TestStreamingPage<>(
            HttpHeaders.EMPTY,
            "",
            Flux
                .<StreamingPage.Chunk<HttpHeaders>>fromIterable(List.of(
                    StreamingPage.Chunk.header(HEADERS_1, CONTENT_TYPE_1),
                    StreamingPage.Chunk.bodyChunk(utf8("ab")),
                    StreamingPage.Chunk.bodyEnd()
                ))
                .concatWith(Mono.error(expectedException))
        );

        final var result = JdkFlowAdapter.flowPublisherToFlux(streamingPage.toCompleteEntities());

        StepVerifier
            .create(result)
            .expectNext(new Entity<>(HEADERS_1,
                                     Body.fromBytes("ab".getBytes(StandardCharsets.UTF_8), CONTENT_TYPE_1)))
            .expectErrorMatches(expectedException::equals)
            .verify();
    }

    @Test
    void toCompletePage_shouldGetPage() throws ExecutionException, InterruptedException {
        final TestStreamingPage<HttpHeaders, HttpHeaders> streamingPage = new TestStreamingPage<>(
            HttpHeaders.EMPTY,
            "_---_bnd",
            List.of(StreamingPage.Chunk.header(HEADERS_1, CONTENT_TYPE_1),
                    StreamingPage.Chunk.bodyChunk(utf8("abc")),
                    StreamingPage.Chunk.bodyChunk(utf8("def")),
                    StreamingPage.Chunk.bodyEnd(),
                    StreamingPage.Chunk.header(HEADERS_2, CONTENT_TYPE_2),
                    StreamingPage.Chunk.bodyChunk(utf8("123")),
                    StreamingPage.Chunk.bodyChunk(utf8("456")),
                    StreamingPage.Chunk.bodyChunk(utf8("78")),
                    StreamingPage.Chunk.bodyEnd()));

        final var result = streamingPage
            .toCompletePage()
            .toCompletableFuture()
            .get();

        assertThat(result).isEqualTo(new Page<>(
            HttpHeaders.EMPTY,
            "_---_bnd", List.of(
            new Entity<>(HEADERS_1, Body.fromBytes("abcdef".getBytes(StandardCharsets.UTF_8), CONTENT_TYPE_1)),
            new Entity<>(HEADERS_2, Body.fromBytes("12345678".getBytes(StandardCharsets.UTF_8), CONTENT_TYPE_2)))
        ));
    }

    @Test
    void toCompletePage_shouldPassThroughExceptionFromPage() {
        final var expectedException = new HttpException.NetworkError(Url.of(""), new IOException("whoops"));
        final var streamingPage = new TestStreamingPage<>(
            HttpHeaders.EMPTY,
            "",
            Flux.error(expectedException)
        );

        final var result = Mono.fromCompletionStage(streamingPage.toCompletePage());

        StepVerifier
            .create(result)
            .expectErrorMatches(expectedException::equals)
            .verify();
    }
}
