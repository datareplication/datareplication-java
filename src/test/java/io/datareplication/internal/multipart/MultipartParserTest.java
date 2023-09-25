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

    private static List<Elem> parseExactly(MultipartParser parser, ByteBuffer input) {
        final ArrayList<Elem> list = new ArrayList<>();
        while (input.hasRemaining()) {
            final Result result = parser.parse(input.slice());
            list.add(result.elem());
            input.position(input.position() + result.consumedBytes());
        }
        return list;
    }

    @Test
    public void shouldParseMultipartBodyDetailed() {
        MultipartParser parser = new MultipartParser(utf8("_---_boundary"));

        assertThat(parser.parse(utf8("preamble\r\n")))
                .isEqualTo(new Result(Elem.Continue.INSTANCE, 10));
        assertThat(parser.parse(utf8("--_---_boundary")))
                .isEqualTo(new Result(Elem.Continue.INSTANCE, 15));
        assertThat(parser.parse(utf8("\r\nignored...")))
                .isEqualTo(new Result(Elem.PartBegin.INSTANCE, 2));
        assertThat(parser.parse(utf8("  Header  :   value\n")))
                .isEqualTo(new Result(new Elem.Header("Header", "value"), 20));
        assertThat(parser.parse(utf8("h:v\r\n")))
                .isEqualTo(new Result(new Elem.Header("h", "v"), 5));
        assertThat(parser.parse(utf8("\r\nignored...")))
                .isEqualTo(new Result(Elem.DataBegin.INSTANCE, 2));
        assertThat(parser.parse(utf8("datadatadata")))
                .isEqualTo(new Result(new Elem.Data(utf8("datadatadata")), 12));
        assertThat(parser.parse(utf8("\r\nmore\ndata")))
                .isEqualTo(new Result(new Elem.Data(utf8("\r\nmore")), 6));
        assertThat(parser.parse(utf8("\ndata")))
                .isEqualTo(new Result(new Elem.Data(utf8("\ndata")), 5));
        assertThat(parser.parse(utf8("\r\n\n--_---_boundary")))
                .isEqualTo(new Result(Elem.PartEnd.INSTANCE, 18));
        assertThat(parser.parse(utf8("\n")))
                .isEqualTo(new Result(Elem.PartBegin.INSTANCE, 1));
        assertThat(parser.parse(utf8("\r\n")))
                .isEqualTo(new Result(Elem.DataBegin.INSTANCE, 2));
        assertThat(parser.parse(utf8("data")))
                .isEqualTo(new Result(new Elem.Data(utf8("data")), 4));
        assertThat(parser.parse(utf8("\r\n\r\n--_---_boundary")))
                .isEqualTo(new Result(Elem.PartEnd.INSTANCE, 19));
        assertThat(parser.parse(utf8("--")))
                .isEqualTo(new Result(Elem.Continue.INSTANCE, 2));
        assertThat(parser.parse(utf8("epilogue")))
                .isEqualTo(new Result(Elem.Continue.INSTANCE, 8));
    }

    @Test
    public void shouldParseMultipartBody() {
        MultipartParser parser = new MultipartParser(utf8("..."));

        assertThat(parseExactly(parser, utf8("--...\nh1:v1\nh2:v2\n\ndatadatadata\n\n--...--")))
                .containsExactly(Elem.Continue.INSTANCE,
                                 Elem.PartBegin.INSTANCE,
                                 new Elem.Header("h1", "v1"),
                                 new Elem.Header("h2", "v2"),
                                 Elem.DataBegin.INSTANCE,
                                 new Elem.Data(utf8("datadatadata")),
                                 Elem.PartEnd.INSTANCE,
                                 Elem.Continue.INSTANCE);
    }

    @Test
    public void shouldIgnoreNotQuiteCorrectDelimiter() {
        MultipartParser parser = new MultipartParser(utf8("_---_boundary"));

        assertThat(parseExactly(parser, utf8("--_---_boundary\r\n\r\ndata data\r\n\r\n--_---_boundarz\n\n--_---_boundary--")))
                .containsExactly(Elem.Continue.INSTANCE,
                                 Elem.PartBegin.INSTANCE,
                                 Elem.DataBegin.INSTANCE,
                                 new Elem.Data(utf8("data data")),
                                 new Elem.Data(utf8("\r\n")),
                                 new Elem.Data(utf8("\r\n--_---_boundarz")),
                                 Elem.PartEnd.INSTANCE,
                                 Elem.Continue.INSTANCE);
    }

    @Test
    public void shouldIgnoreDelimiterWithOnlyOneNewline() {
        MultipartParser parser = new MultipartParser(utf8("_b"));

        assertThat(parseExactly(parser, utf8("--_b\n\ndata\n--_b-- other stuff\n\n--_b--")))
                .containsExactly(Elem.Continue.INSTANCE,
                                 Elem.PartBegin.INSTANCE,
                                 Elem.DataBegin.INSTANCE,
                                 new Elem.Data(utf8("data")),
                                 new Elem.Data(utf8("\n--_b-- other stuff")),
                                 Elem.PartEnd.INSTANCE,
                                 Elem.Continue.INSTANCE);
    }

    @Test
    public void shouldParseEmptyMultipart() {
        MultipartParser parser = new MultipartParser(utf8("bnd"));

        assertThat(parseExactly(parser, utf8("--bnd--epilogue more stuff")))
                .containsExactly(Elem.Continue.INSTANCE,
                                 Elem.Continue.INSTANCE,
                                 Elem.Continue.INSTANCE);
    }

    @Test
    public void shouldRequireNewlineAfterPrologue() {
        MultipartParser parser = new MultipartParser(utf8("_b"));

        assertThat(parseExactly(parser, utf8("fake start: --_b\n\nactual start:\n--_b\n\n\n\n--_b--")))
                .containsExactly(Elem.Continue.INSTANCE,
                                 Elem.Continue.INSTANCE,
                                 Elem.Continue.INSTANCE,
                                 Elem.Continue.INSTANCE,
                                 Elem.PartBegin.INSTANCE,
                                 Elem.DataBegin.INSTANCE,
                                 Elem.PartEnd.INSTANCE,
                                 Elem.Continue.INSTANCE);
    }

    @Test
    public void shouldTrimHeaderWhitespace() {
        MultipartParser parser = new MultipartParser(utf8("_b"));

        assertThat(parseExactly(parser, utf8("--_b\nh1:v1\n   h2      :    v2    \nh 3: v 3\n\n\n\n--_b--")))
                .containsExactly(Elem.Continue.INSTANCE,
                                 Elem.PartBegin.INSTANCE,
                                 new Elem.Header("h1", "v1"),
                                 new Elem.Header("h2", "v2"),
                                 new Elem.Header("h 3", "v 3"),
                                 Elem.DataBegin.INSTANCE,
                                 Elem.PartEnd.INSTANCE,
                                 Elem.Continue.INSTANCE);
    }

    @Test
    public void shouldRequestMoreInput_whenIncompleteInitialDelimiter() {
        MultipartParser parser = new MultipartParser(utf8("_b"));

        assertThatThrownBy(() -> parseExactly(parser, utf8("--_")))
                .isInstanceOf(RequestInput.class);
    }

    @Test
    public void shouldRequestMoreInput_whenIncompleteLaterDelimiter() {
        MultipartParser parser = new MultipartParser(utf8("_b"));

        assertThatThrownBy(() -> parseExactly(parser, utf8("--_b\n\n\n\n--_")))
                .isInstanceOf(RequestInput.class);
    }

    @Test
    public void shouldRequestMoreInput_whenIncompleteCloseDelimiter() {
        MultipartParser parser = new MultipartParser(utf8("_b"));

        assertThatThrownBy(() -> parseExactly(parser, utf8("--_b-")))
                .isInstanceOf(RequestInput.class);
    }

    @Test
    public void shouldRequestMoreInput_whenUndelimitedHeader() {
        MultipartParser parser = new MultipartParser(utf8("_b"));

        assertThatThrownBy(() -> parseExactly(parser, utf8("--_b\nh1: v1\nh2: v2    --_b--")))
                .isInstanceOf(RequestInput.class);
    }

    @Test
    public void shouldError_whenInvalidInitialDelimiter() {
        MultipartParser parser = new MultipartParser(utf8("_b"));

        assertThatThrownBy(() -> parseExactly(parser, utf8("--_b... oh no!")))
                .isEqualTo(new MultipartException.InvalidDelimiter(4));
    }

    @Test
    public void shouldError_whenInvalidCloseDelimiter() {
        MultipartParser parser = new MultipartParser(utf8("_b"));

        assertThatThrownBy(() -> parseExactly(parser, utf8("--_b\n\n\n\n--_b-.")))
                .isEqualTo(new MultipartException.InvalidDelimiter(12));
    }

    @Test
    public void shouldError_whenHeaderLineWithoutColon() {
        MultipartParser parser = new MultipartParser(utf8("_b"));

        assertThatThrownBy(() -> parseExactly(parser, utf8("--_b\nheader... oops\n\n\n--_b--")))
                .isEqualTo(new MultipartException.InvalidHeader("header... oops", 5));
    }

    @Test
    public void shouldError_whenUndecodableHeader() {
        MultipartParser parser = new MultipartParser(utf8("_b"), StandardCharsets.US_ASCII);

        assertThatThrownBy(() -> parseExactly(parser, utf8("--_b\nheader: äö\n\n\n--_b--")))
                .isEqualTo(new MultipartException.UndecodableHeader(StandardCharsets.US_ASCII, 5));
    }
}
