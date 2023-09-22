package io.datareplication.internal.multipart;

import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import static io.datareplication.internal.multipart.MultipartParser.Result;
import static org.assertj.core.api.Assertions.assertThat;


class MultipartParserTest {
    private static ByteBuffer utf8(String s) {
        return ByteBuffer.wrap(s.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    public void shouldParseMultipartBody() throws RequestInput {
        MultipartParser parser = new MultipartParser(utf8("_---_boundary"));

        assertThat(parser.parse(utf8("preamble")))
            .isEqualTo(new Result(Elem.Continue.INSTANCE, 8));
        assertThat(parser.parse(utf8("\r\n more stuff \n\r")))
            .isEqualTo(new Result(Elem.Continue.INSTANCE, 16));
        assertThat(parser.parse(utf8("stuff--_---_boundary")))
            .isEqualTo(new Result(Elem.Continue.INSTANCE, 20));
        assertThat(parser.parse(utf8("\r\nignored...")))
            .isEqualTo(new Result(Elem.PartBegin.INSTANCE, 2));
        assertThat(parser.parse(utf8("  Header  :   value\n")))
            .isEqualTo(new Result(new Elem.Header("Header", "value"), 20));
        assertThat(parser.parse(utf8("h:v\r\n")))
            .isEqualTo(new Result(new Elem.Header("h", "v"), 5));
        assertThat(parser.parse(utf8("\r\n")))
            .isEqualTo(new Result(new Elem.Data(utf8("")), 2));
        assertThat(parser.parse(utf8("datadatadata")))
            .isEqualTo(new Result(new Elem.Data(utf8("datadatadata")), 12));
        assertThat(parser.parse(utf8("\r\nmore\ndata")))
            .isEqualTo(new Result(new Elem.Data(utf8("\r\nmore\ndata")), 11));
        assertThat(parser.parse(utf8("x\r\n\n--_---_boundary")))
            .isEqualTo(new Result(new Elem.Data(utf8("x")), 19));
        assertThat(parser.parse(utf8("ignored...")))
            .isEqualTo(new Result(Elem.PartEnd.INSTANCE, 0));
        assertThat(parser.parse(utf8("\n")))
            .isEqualTo(new Result(Elem.PartBegin.INSTANCE, 1));
        assertThat(parser.parse(utf8("\r\n")))
            .isEqualTo(new Result(new Elem.Data(utf8("")), 2));
        assertThat(parser.parse(utf8("data")))
            .isEqualTo(new Result(new Elem.Data(utf8("data")), 4));
        assertThat(parser.parse(utf8("\r\n\r\n--_---_boundary")))
            .isEqualTo(new Result(new Elem.Data(utf8("")), 19));
        assertThat(parser.parse(utf8("ignored...")))
            .isEqualTo(new Result(Elem.PartEnd.INSTANCE, 0));
        assertThat(parser.parse(utf8("--")))
            .isEqualTo(new Result(Elem.Continue.INSTANCE, 2));
        assertThat(parser.parse(utf8("epilogue")))
            .isEqualTo(new Result(Elem.Continue.INSTANCE, 8));
    }
}
