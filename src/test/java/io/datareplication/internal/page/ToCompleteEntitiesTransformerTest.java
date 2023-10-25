package io.datareplication.internal.page;

import io.datareplication.consumer.StreamingPage;
import io.datareplication.model.Body;
import io.datareplication.model.ContentType;
import io.datareplication.model.Entity;
import io.datareplication.model.HttpHeader;
import io.datareplication.model.HttpHeaders;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class ToCompleteEntitiesTransformerTest {
    private final ToCompleteEntitiesTransformer<HttpHeaders> transformer = new ToCompleteEntitiesTransformer<>();

    private static final HttpHeaders HEADERS_1 = HttpHeaders.of(
        HttpHeader.of("h1", "v1"),
        HttpHeader.of("h2", "v2"));
    private static final HttpHeaders HEADERS_2 = HttpHeaders.EMPTY;

    private static final ContentType CONTENT_TYPE_1 = ContentType.of("application/json");
    private static final ContentType CONTENT_TYPE_2 = ContentType.of("audio/flac");

    private static ByteBuffer utf8(String s) {
        return ByteBuffer.wrap(s.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void shouldTransformStreamOfChunks() {
        assertThat(transformer.transform(StreamingPage.Chunk.header(HEADERS_1, CONTENT_TYPE_1))).isEmpty();
        assertThat(transformer.transform(StreamingPage.Chunk.bodyChunk(utf8("a")))).isEmpty();
        assertThat(transformer.transform(StreamingPage.Chunk.bodyChunk(utf8("b")))).isEmpty();
        assertThat(transformer.transform(StreamingPage.Chunk.bodyChunk(utf8("c")))).isEmpty();
        assertThat(transformer.transform(StreamingPage.Chunk.bodyEnd()))
            .contains(new Entity<>(
                HEADERS_1,
                Body.fromBytes("abc".getBytes(StandardCharsets.UTF_8), CONTENT_TYPE_1)));
        assertThat(transformer.transform(StreamingPage.Chunk.header(HEADERS_2, CONTENT_TYPE_2))).isEmpty();
        assertThat(transformer.transform(StreamingPage.Chunk.bodyChunk(utf8("test")))).isEmpty();
        assertThat(transformer.transform(StreamingPage.Chunk.bodyEnd()))
            .contains(new Entity<>(
                HEADERS_2,
                Body.fromBytes("test".getBytes(StandardCharsets.UTF_8), CONTENT_TYPE_2)));
    }
}
