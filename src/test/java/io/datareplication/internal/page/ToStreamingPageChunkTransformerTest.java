package io.datareplication.internal.page;

import io.datareplication.consumer.PageFormatException;
import io.datareplication.consumer.StreamingPage;
import io.datareplication.internal.multipart.Token;
import io.datareplication.model.ContentType;
import io.datareplication.model.HttpHeader;
import io.datareplication.model.HttpHeaders;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ToStreamingPageChunkTransformerTest {
    private final ToStreamingPageChunkTransformer transformer = new ToStreamingPageChunkTransformer();

    private static final ByteBuffer BYTES_1 = ByteBuffer.wrap("test1".getBytes(StandardCharsets.UTF_8));
    private static final ByteBuffer BYTES_2 = ByteBuffer.wrap("test2".getBytes(StandardCharsets.UTF_8));

    @Test
    void shouldTransformStreamOfTokens() {
        assertThat(transformer.transform(Token.PartBegin.INSTANCE)).isEmpty();
        assertThat(transformer.transform(new Token.Header("a", "b"))).isEmpty();
        assertThat(transformer.transform(new Token.Header("content-type", "text/plain"))).isEmpty();
        assertThat(transformer.transform(new Token.Header("b", "c"))).isEmpty();
        assertThat(transformer.transform(Token.DataBegin.INSTANCE))
            .contains(StreamingPage.Chunk.header(
                HttpHeaders.of(HttpHeader.of("a", "b"),
                               HttpHeader.of("b", "c")),
                ContentType.of("text/plain")));
        assertThat(transformer.transform(new Token.Data(BYTES_1)))
            .contains(StreamingPage.Chunk.bodyChunk(BYTES_1));
        assertThat(transformer.transform(new Token.Data(BYTES_2)))
            .contains(StreamingPage.Chunk.bodyChunk(BYTES_2));
        assertThat(transformer.transform(Token.PartEnd.INSTANCE))
            .contains(StreamingPage.Chunk.bodyEnd());
        assertThat(transformer.transform(Token.PartBegin.INSTANCE)).isEmpty();
        assertThat(transformer.transform(new Token.Header("content-type", "application/octet-stream"))).isEmpty();
        assertThat(transformer.transform(Token.DataBegin.INSTANCE))
            .contains(StreamingPage.Chunk.header(
                HttpHeaders.EMPTY,
                ContentType.of("application/octet-stream")));
        assertThat(transformer.transform(new Token.Data(BYTES_1)))
            .contains(StreamingPage.Chunk.bodyChunk(BYTES_1));
        assertThat(transformer.transform(Token.PartEnd.INSTANCE))
            .contains(StreamingPage.Chunk.bodyEnd());
    }

    @Test
    void shouldIgnoreContinue() {
        assertThat(transformer.transform(Token.Continue.INSTANCE)).isEmpty();
    }

    @Test
    void shouldRequireContentTypeHeader() {
        transformer.transform(Token.PartBegin.INSTANCE);
        transformer.transform(new Token.Header("content-type", "text/plain"));
        transformer.transform(Token.DataBegin.INSTANCE);
        transformer.transform(Token.PartEnd.INSTANCE);
        transformer.transform(Token.PartBegin.INSTANCE);
        transformer.transform(new Token.Header("a", "b"));
        transformer.transform(new Token.Header("not-content-type", "text/plain"));
        transformer.transform(new Token.Header("b", "c"));
        assertThatThrownBy(() -> transformer.transform(Token.DataBegin.INSTANCE))
            .isEqualTo(new PageFormatException.MissingContentTypeInEntity(1));
    }

    @Test
    void shouldCompareContentTypeHeaderNameIgnoringCapitalization() {
        transformer.transform(Token.PartBegin.INSTANCE);
        transformer.transform(new Token.Header("Content-Type", "text/plain"));
        assertThat(transformer.transform(Token.DataBegin.INSTANCE))
            .contains(StreamingPage.Chunk.header(HttpHeaders.EMPTY,
                                                 ContentType.of("text/plain")));
        transformer.transform(Token.PartEnd.INSTANCE);

        transformer.transform(Token.PartBegin.INSTANCE);
        transformer.transform(new Token.Header("cONTenT-tYpE", "audio/mp3"));
        assertThat(transformer.transform(Token.DataBegin.INSTANCE))
            .contains(StreamingPage.Chunk.header(HttpHeaders.EMPTY,
                                                 ContentType.of("audio/mp3")));
    }
}
