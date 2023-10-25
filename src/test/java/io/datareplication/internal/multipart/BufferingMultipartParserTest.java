package io.datareplication.internal.multipart;

import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BufferingMultipartParserTest {
    private static ByteBuffer utf8(String s) {
        return ByteBuffer.wrap(s.getBytes(StandardCharsets.UTF_8));
    }

    private final BufferingMultipartParser parser = new BufferingMultipartParser(new MultipartParser(utf8("bb")));

    @Test
    void shouldParseSingleCompleteBuffer() {
        final ByteBuffer input = utf8("prologue, who cares\n--bb\nheader:value\n\nbodybodybody\n\n--bb--");

        assertThat(parser.parse(input))
            .containsExactly(Token.Continue.INSTANCE,
                             Token.Continue.INSTANCE,
                             Token.PartBegin.INSTANCE,
                             new Token.Header("header", "value"),
                             Token.DataBegin.INSTANCE,
                             new Token.Data(utf8("bodybodybody")),
                             Token.PartEnd.INSTANCE,
                             Token.Continue.INSTANCE);
        parser.finish();
    }

    @Test
    void shouldParseIncrementalCompleteBuffers() {
        assertThat(parser.parse(utf8("prologue\n--bb\nheader")))
            .containsExactly(Token.Continue.INSTANCE,
                             Token.Continue.INSTANCE,
                             Token.PartBegin.INSTANCE);
        assertThat(parser.parse(utf8(": value\n")))
            .containsExactly(new Token.Header("header", "value"));
        assertThat(parser.parse(utf8("header2:"))).isEmpty();
        assertThat(parser.parse(utf8(""))).isEmpty();
        assertThat(parser.parse(utf8(" more value"))).isEmpty();
        assertThat(parser.parse(utf8(""))).isEmpty();
        assertThat(parser.parse(utf8("\n")))
            .containsExactly(new Token.Header("header2", "more value"));
        assertThat(parser.parse(utf8("\nbody body body")))
            .containsExactly(Token.DataBegin.INSTANCE,
                             new Token.Data(utf8("body body body")));
        assertThat(parser.parse(utf8("\n\n--bb")))
            .containsExactly(Token.PartEnd.INSTANCE);
        assertThat(parser.parse(utf8("\nh:v\n\n\n\n--bb")))
            .containsExactly(Token.PartBegin.INSTANCE,
                             new Token.Header("h", "v"),
                             Token.DataBegin.INSTANCE,
                             Token.PartEnd.INSTANCE);
        assertThat(parser.parse(utf8("--")))
            .containsExactly(Token.Continue.INSTANCE);
        parser.finish();
    }

    @Test
    void shouldPartiallyParseSingleIncompleteBuffer() {
        final ByteBuffer input = utf8("--bb\n\nbody\n\n--");

        assertThat(parser.parse(input))
            .containsExactly(Token.Continue.INSTANCE,
                             Token.PartBegin.INSTANCE,
                             Token.DataBegin.INSTANCE,
                             new Token.Data(utf8("body")));
        assertThatThrownBy(parser::finish)
            .isEqualTo(new MultipartException.UnexpectedEndOfInput(10));
    }

    @Test
    void shouldPartiallyParseBufferAndThrowException_whenInvalidMultipartDelimiter() {
        final ByteBuffer input = utf8("--bb\n\nbody\n\n--bb...oops");

        assertThat(parser.parse(input))
            .containsExactly(Token.Continue.INSTANCE,
                             Token.PartBegin.INSTANCE,
                             Token.DataBegin.INSTANCE,
                             new Token.Data(utf8("body")),
                             Token.PartEnd.INSTANCE);
        assertThatThrownBy(parser::finish)
            .isEqualTo(new MultipartException.InvalidDelimiter(16));
    }

    @Test
    void shouldPartiallyParseBufferAndThrowException_whenInputConsumedButNotInTerminationState() {
        final ByteBuffer input = utf8("--bb\n\nbody\n\n--bb");

        assertThat(parser.parse(input))
            .containsExactly(Token.Continue.INSTANCE,
                             Token.PartBegin.INSTANCE,
                             Token.DataBegin.INSTANCE,
                             new Token.Data(utf8("body")),
                             Token.PartEnd.INSTANCE);
        assertThatThrownBy(parser::finish)
            .isEqualTo(new MultipartException.UnexpectedEndOfInput(16));
    }
}
