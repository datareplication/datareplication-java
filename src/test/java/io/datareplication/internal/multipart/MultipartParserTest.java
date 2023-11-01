package io.datareplication.internal.multipart;

import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static io.datareplication.internal.multipart.MultipartParser.Result;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


class MultipartParserTest {
    private static ByteBuffer utf8(String s) {
        return ByteBuffer.wrap(s.getBytes(StandardCharsets.UTF_8));
    }

    private static List<Token> parseExactly(MultipartParser parser, ByteBuffer input) {
        final ArrayList<Token> list = new ArrayList<>();
        while (input.hasRemaining()) {
            final Result result = parser.parse(input);
            list.add(result.token());
            input.position(input.position() + result.consumedBytes());
        }
        return list;
    }

    @Test
    void shouldParseMultipartBodyDetailed() {
        MultipartParser parser = new MultipartParser(utf8("_---_boundary"));

        assertThat(parser.parse(utf8("preamble\r\n")))
                .isEqualTo(new Result(Token.Continue.INSTANCE, 10));
        assertThat(parser.parse(utf8("--_---_boundary")))
                .isEqualTo(new Result(Token.Continue.INSTANCE, 15));
        assertThat(parser.parse(utf8("\r\nignored...")))
                .isEqualTo(new Result(Token.PartBegin.INSTANCE, 2));
        assertThat(parser.parse(utf8("  Header  :   value\n")))
                .isEqualTo(new Result(new Token.Header("Header", "value"), 20));
        assertThat(parser.parse(utf8("h:v\r\n")))
                .isEqualTo(new Result(new Token.Header("h", "v"), 5));
        assertThat(parser.parse(utf8("\r\nignored...")))
                .isEqualTo(new Result(Token.DataBegin.INSTANCE, 2));
        assertThat(parser.parse(utf8("datadatadata")))
                .isEqualTo(new Result(new Token.Data(utf8("datadatadata")), 12));
        assertThat(parser.parse(utf8("\r\nmore\ndata")))
                .isEqualTo(new Result(new Token.Data(utf8("\r\nmore")), 6));
        assertThat(parser.parse(utf8("\ndata")))
                .isEqualTo(new Result(new Token.Data(utf8("\ndata")), 5));
        assertThat(parser.parse(utf8("\r\n--_---_boundary")))
                .isEqualTo(new Result(Token.PartEnd.INSTANCE, 17));
        assertThat(parser.parse(utf8("\n")))
                .isEqualTo(new Result(Token.PartBegin.INSTANCE, 1));
        assertThat(parser.parse(utf8("\r\n")))
                .isEqualTo(new Result(Token.DataBegin.INSTANCE, 2));
        assertThat(parser.parse(utf8("data")))
                .isEqualTo(new Result(new Token.Data(utf8("data")), 4));
        assertThat(parser.parse(utf8("\r\n--_---_boundary")))
                .isEqualTo(new Result(Token.PartEnd.INSTANCE, 17));
        assertThat(parser.parse(utf8("--")))
                .isEqualTo(new Result(Token.Continue.INSTANCE, 2));
        assertThat(parser.parse(utf8("epilogue")))
                .isEqualTo(new Result(Token.Continue.INSTANCE, 8));
        assertThat(parser.isFinished()).isTrue();
    }

    @Test
    void shouldParseMultipartBody() {
        MultipartParser parser = new MultipartParser(utf8("..."));

        assertThat(parseExactly(parser, utf8("--...\nh1:v1\nh2:v2\n\ndatadatadata\n--...--")))
                .containsExactly(Token.Continue.INSTANCE,
                                 Token.PartBegin.INSTANCE,
                                 new Token.Header("h1", "v1"),
                                 new Token.Header("h2", "v2"),
                                 Token.DataBegin.INSTANCE,
                                 new Token.Data(utf8("datadatadata")),
                                 Token.PartEnd.INSTANCE,
                                 Token.Continue.INSTANCE);
        assertThat(parser.isFinished()).isTrue();
    }

    @Test
    void shouldSkipPastNotQuiteCorrectDelimiter() {
        MultipartParser parser = new MultipartParser(utf8("_---_boundary"));

        final String multipart = "--_---_boundary\r\n\r\ndata data\r\n\r\n--_---_boundarz\n--_---_boundary--";
        assertThat(parseExactly(parser, utf8(multipart)))
                .containsExactly(Token.Continue.INSTANCE,
                                 Token.PartBegin.INSTANCE,
                                 Token.DataBegin.INSTANCE,
                                 new Token.Data(utf8("data data")),
                                 new Token.Data(utf8("\r\n")),
                                 new Token.Data(utf8("\r\n--_---_boundarz")),
                                 Token.PartEnd.INSTANCE,
                                 Token.Continue.INSTANCE);
        assertThat(parser.isFinished()).isTrue();
    }

    @Test
    void shouldSkipPastDelimiterWithoutNewline() {
        MultipartParser parser = new MultipartParser(utf8("_b"));

        assertThat(parseExactly(parser, utf8("--_b\n\ndata--_b-- other stuff\n--_b--")))
                .containsExactly(Token.Continue.INSTANCE,
                                 Token.PartBegin.INSTANCE,
                                 Token.DataBegin.INSTANCE,
                                 new Token.Data(utf8("data--_b-- other stuff")),
                                 Token.PartEnd.INSTANCE,
                                 Token.Continue.INSTANCE);
        assertThat(parser.isFinished()).isTrue();
    }

    @Test
    void shouldParseBodyOfNewlines() {
        MultipartParser parser = new MultipartParser(utf8("_b"));

        assertThat(parseExactly(parser, utf8("--_b\n\r\n\r\n\n\r\n\n--_b--")))
            .containsExactly(Token.Continue.INSTANCE,
                             Token.PartBegin.INSTANCE,
                             Token.DataBegin.INSTANCE,
                             new Token.Data(utf8("\r\n")),
                             new Token.Data(utf8("\n")),
                             new Token.Data(utf8("\r\n")),
                             Token.PartEnd.INSTANCE,
                             Token.Continue.INSTANCE);
        assertThat(parser.isFinished()).isTrue();
    }

    @Test
    void shouldParseEmptyMultipart() {
        MultipartParser parser = new MultipartParser(utf8("bnd"));

        assertThat(parseExactly(parser, utf8("--bnd--epilogue more stuff")))
                .containsExactly(Token.Continue.INSTANCE,
                                 Token.Continue.INSTANCE,
                                 Token.Continue.INSTANCE);
        assertThat(parser.isFinished()).isTrue();
    }

    @Test
    void shouldRequireNewlineAfterPrologue() {
        MultipartParser parser = new MultipartParser(utf8("_b"));

        assertThat(parseExactly(parser, utf8("fake start: --_b\n\nactual start:\n--_b\n\n\n--_b--")))
                .containsExactly(Token.Continue.INSTANCE,
                                 Token.Continue.INSTANCE,
                                 Token.Continue.INSTANCE,
                                 Token.Continue.INSTANCE,
                                 Token.PartBegin.INSTANCE,
                                 Token.DataBegin.INSTANCE,
                                 Token.PartEnd.INSTANCE,
                                 Token.Continue.INSTANCE);
        assertThat(parser.isFinished()).isTrue();
    }

    @Test
    void shouldTrimHeaderWhitespace() {
        MultipartParser parser = new MultipartParser(utf8("_b"));

        assertThat(parseExactly(parser, utf8("--_b\nh1:v1\n   h2      :    v2    \nh 3: v 3\n\n\n--_b--")))
                .containsExactly(Token.Continue.INSTANCE,
                                 Token.PartBegin.INSTANCE,
                                 new Token.Header("h1", "v1"),
                                 new Token.Header("h2", "v2"),
                                 new Token.Header("h 3", "v 3"),
                                 Token.DataBegin.INSTANCE,
                                 Token.PartEnd.INSTANCE,
                                 Token.Continue.INSTANCE);
        assertThat(parser.isFinished()).isTrue();
    }

    @Test
    void shouldRequestMoreInput_whenIncompleteInitialDelimiter() {
        MultipartParser parser = new MultipartParser(utf8("_b"));

        assertThatThrownBy(() -> parseExactly(parser, utf8("--_")))
                .isInstanceOf(RequestInput.class);
    }

    @Test
    void shouldRequestMoreInput_whenIncompleteLaterDelimiter() {
        MultipartParser parser = new MultipartParser(utf8("_b"));

        assertThatThrownBy(() -> parseExactly(parser, utf8("--_b\n\n\n\n--_")))
                .isInstanceOf(RequestInput.class);
    }

    @Test
    void shouldRequestMoreInput_whenIncompleteCloseDelimiter() {
        MultipartParser parser = new MultipartParser(utf8("_b"));

        assertThatThrownBy(() -> parseExactly(parser, utf8("--_b-")))
                .isInstanceOf(RequestInput.class);
    }

    @Test
    void shouldRequestMoreInput_whenUndelimitedHeader() {
        MultipartParser parser = new MultipartParser(utf8("_b"));

        assertThatThrownBy(() -> parseExactly(parser, utf8("--_b\nh1: v1\nh2: v2    --_b--")))
                .isInstanceOf(RequestInput.class);
    }

    @Test
    void shouldError_whenInvalidInitialDelimiter() {
        MultipartParser parser = new MultipartParser(utf8("_b"));

        assertThatThrownBy(() -> parseExactly(parser, utf8("--_b... oh no!")))
                .isEqualTo(new MultipartException.InvalidDelimiter(4));
    }

    @Test
    void shouldError_whenInvalidCloseDelimiter() {
        MultipartParser parser = new MultipartParser(utf8("_b"));

        assertThatThrownBy(() -> parseExactly(parser, utf8("--_b\n\n\n\n--_b-.")))
                .isEqualTo(new MultipartException.InvalidDelimiter(12));
    }

    @Test
    void shouldError_whenHeaderLineWithoutColon() {
        MultipartParser parser = new MultipartParser(utf8("_b"));

        assertThatThrownBy(() -> parseExactly(parser, utf8("--_b\nheader... oops\n\n\n--_b--")))
                .isEqualTo(new MultipartException.InvalidHeader("header... oops", 5));
    }

    @Test
    void shouldError_whenUndecodableHeader() {
        MultipartParser parser = new MultipartParser(utf8("_b"), StandardCharsets.US_ASCII);

        assertThatThrownBy(() -> parseExactly(parser, utf8("--_b\nheader: äö\n\n\n--_b--")))
                .isEqualTo(new MultipartException.UndecodableHeader(StandardCharsets.US_ASCII, 5));
    }

    @Test
    void shouldNotBeFinished_whenInPrologue() {
        MultipartParser parser = new MultipartParser(utf8("_b"));

        parseExactly(parser, utf8("prologue... --_b\noops, fakeout"));
        assertThat(parser.isFinished()).isFalse();
    }

    @Test
    void shouldNotBeFinished_whenOpenDelimiterWithoutNewline() {
        MultipartParser parser = new MultipartParser(utf8("_b"));

        parseExactly(parser, utf8("--_b"));
        assertThat(parser.isFinished()).isFalse();
    }

    @Test
    void shouldNotBeFinished_whenUnterminatedHeaders() {
        MultipartParser parser = new MultipartParser(utf8("_b"));

        parseExactly(parser, utf8("--_b\nh1: v1\n"));
        assertThat(parser.isFinished()).isFalse();
    }

    @Test
    void shouldNotBeFinished_whenUnterminatedData() {
        MultipartParser parser = new MultipartParser(utf8("_b"));

        parseExactly(parser, utf8("--_b\n\ndatadatadata"));
        assertThat(parser.isFinished()).isFalse();
    }

    @Test
    void shouldNotBeFinished_whenIncompleteCloseDelimiter() {
        MultipartParser parser = new MultipartParser(utf8("_b"));

        parseExactly(parser, utf8("--_b\nh1:v1\n\ndata\n\n--_b"));
        assertThat(parser.isFinished()).isFalse();
    }
}
