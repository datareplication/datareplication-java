package io.datareplication.internal.multipart;

import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class BufferingMultipartParserTest {
    private static ByteBuffer utf8(String s) {
        return ByteBuffer.wrap(s.getBytes(StandardCharsets.UTF_8));
    }

    private final BufferingMultipartParser transformer = new BufferingMultipartParser(new MultipartParser(utf8("bb")));

    @Test
    void shouldParseSingleCompleteBuffer() {
        final ByteBuffer input = utf8("prologue, who cares\n--bb\nheader:value\n\nbodybodybody\n\n--bb--");

        assertThat(transformer.parse(input))
            .containsExactly(Token.Continue.INSTANCE,
                             Token.Continue.INSTANCE,
                             Token.PartBegin.INSTANCE,
                             new Token.Header("header", "value"),
                             Token.DataBegin.INSTANCE,
                             new Token.Data(utf8("bodybodybody")),
                             Token.PartEnd.INSTANCE,
                             Token.Continue.INSTANCE);
        assertThat(transformer.isFinished()).isTrue();
    }

    @Test
    void shouldParseIncrementalCompleteBuffers() {
        assertThat(transformer.parse(utf8("prologue\n--bb\nheader")))
            .containsExactly(Token.Continue.INSTANCE,
                             Token.Continue.INSTANCE,
                             Token.PartBegin.INSTANCE);
        assertThat(transformer.isFinished()).isFalse();
        assertThat(transformer.parse(utf8(": value\n")))
            .containsExactly(new Token.Header("header", "value"));
        assertThat(transformer.isFinished()).isFalse();
        assertThat(transformer.parse(utf8("header2:"))).isEmpty();
        assertThat(transformer.isFinished()).isFalse();
        assertThat(transformer.parse(utf8(""))).isEmpty();
        assertThat(transformer.isFinished()).isFalse();
        assertThat(transformer.parse(utf8(" more value"))).isEmpty();
        assertThat(transformer.isFinished()).isFalse();
        assertThat(transformer.parse(utf8(""))).isEmpty();
        assertThat(transformer.isFinished()).isFalse();
        assertThat(transformer.parse(utf8("\n")))
            .containsExactly(new Token.Header("header2", "more value"));
        assertThat(transformer.isFinished()).isFalse();
        assertThat(transformer.parse(utf8("\nbody body body")))
            .containsExactly(Token.DataBegin.INSTANCE,
                             new Token.Data(utf8("body body body")));
        assertThat(transformer.isFinished()).isFalse();
        assertThat(transformer.parse(utf8("\n\n--bb")))
            .containsExactly(Token.PartEnd.INSTANCE);
        assertThat(transformer.isFinished()).isFalse();
        assertThat(transformer.parse(utf8("\nh:v\n\n\n\n--bb")))
            .containsExactly(Token.PartBegin.INSTANCE,
                             new Token.Header("h", "v"),
                             Token.DataBegin.INSTANCE,
                             Token.PartEnd.INSTANCE);
        assertThat(transformer.isFinished()).isFalse();
        assertThat(transformer.parse(utf8("--")))
            .containsExactly(Token.Continue.INSTANCE);
        assertThat(transformer.isFinished()).isTrue();
    }

    @Test
    void shouldPartiallyParseSingleIncompleteBuffer() {
        final ByteBuffer input = utf8("--bb\n\nbody\n\n--");

        assertThat(transformer.parse(input))
            .containsExactly(Token.Continue.INSTANCE,
                             Token.PartBegin.INSTANCE,
                             Token.DataBegin.INSTANCE,
                             new Token.Data(utf8("body")));
        assertThat(transformer.isFinished()).isFalse();
    }
}
