package io.datareplication.consumer;

import io.datareplication.model.Body;
import io.datareplication.model.ContentType;
import io.datareplication.model.Entity;
import io.datareplication.model.HttpHeader;
import io.datareplication.model.HttpHeaders;
import io.datareplication.model.Page;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import lombok.NonNull;
import org.junit.jupiter.api.Test;
import org.reactivestreams.FlowAdapters;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.Flow;

import static org.assertj.core.api.Assertions.assertThat;

class StreamingPageTest {
    private static final HttpHeaders HEADERS_1 = HttpHeaders.of(HttpHeader.of("h1", "v1"));
    private static final ContentType CONTENT_TYPE_1 = ContentType.of("text/plain");
    private static final HttpHeaders HEADERS_2 = HttpHeaders.of(HttpHeader.of("h2", "v2"));
    private static final ContentType CONTENT_TYPE_2 = ContentType.of("audio/mp3");

    private static ByteBuffer utf8(String s) {
        return ByteBuffer.wrap(s.getBytes(StandardCharsets.UTF_8));
    }

    private static final class TestStreamingPage implements StreamingPage<HttpHeaders, HttpHeaders> {
        private final HttpHeaders pageHeader;
        private final String boundary;
        private final List<Chunk<HttpHeaders>> chunks;

        private TestStreamingPage(final HttpHeaders pageHeader,
                                  final String boundary,
                                  final List<Chunk<HttpHeaders>> chunks) {
            this.pageHeader = pageHeader;
            this.boundary = boundary;
            this.chunks = chunks;
        }

        @Override
        public @NonNull HttpHeaders header() {
            return pageHeader;
        }

        @Override
        public @NonNull String boundary() {
            return boundary;
        }

        @Override
        public void subscribe(final Flow.Subscriber<? super Chunk<HttpHeaders>> subscriber) {
            final Flowable<Chunk<HttpHeaders>> flowable = Flowable.fromIterable(chunks);
            FlowAdapters.toFlowPublisher(flowable).subscribe(subscriber);
        }
    }

    @Test
    void shouldStreamCompleteEntities() {
        final TestStreamingPage streamingPage = new TestStreamingPage(HttpHeaders.EMPTY, "", List.of(
            StreamingPage.Chunk.header(HEADERS_1, CONTENT_TYPE_1),
            StreamingPage.Chunk.bodyChunk(utf8("abc")),
            StreamingPage.Chunk.bodyChunk(utf8("def")),
            StreamingPage.Chunk.bodyEnd()));

        final List<@NonNull Entity<HttpHeaders>> result = Flowable
            .fromPublisher(FlowAdapters.toPublisher(streamingPage.toCompleteEntities()))
            .toList()
            .blockingGet();

        assertThat(result).containsExactly(
            new Entity<>(HEADERS_1,
                         Body.fromBytes("abcdef".getBytes(StandardCharsets.UTF_8), CONTENT_TYPE_1)));
    }

    @Test
    void shouldGetCompletePage() {
        final TestStreamingPage streamingPage = new TestStreamingPage(HttpHeaders.EMPTY, "_---_bnd", List.of(
            StreamingPage.Chunk.header(HEADERS_1, CONTENT_TYPE_1),
            StreamingPage.Chunk.bodyChunk(utf8("abc")),
            StreamingPage.Chunk.bodyChunk(utf8("def")),
            StreamingPage.Chunk.bodyEnd(),
            StreamingPage.Chunk.header(HEADERS_2, CONTENT_TYPE_2),
            StreamingPage.Chunk.bodyChunk(utf8("123")),
            StreamingPage.Chunk.bodyChunk(utf8("456")),
            StreamingPage.Chunk.bodyChunk(utf8("78")),
            StreamingPage.Chunk.bodyEnd()));

        final Page<HttpHeaders, HttpHeaders> result = Single
            .fromCompletionStage(streamingPage.toCompletePage())
            .blockingGet();

        assertThat(result).isEqualTo(new Page<>(
            HttpHeaders.EMPTY,
            "_---_bnd", List.of(
                new Entity<>(HEADERS_1, Body.fromBytes("abcdef".getBytes(StandardCharsets.UTF_8), CONTENT_TYPE_1)),
                new Entity<>(HEADERS_2, Body.fromBytes("12345678".getBytes(StandardCharsets.UTF_8), CONTENT_TYPE_2)))
        ));
    }
}
