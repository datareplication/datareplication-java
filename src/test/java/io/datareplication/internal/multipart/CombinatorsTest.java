package io.datareplication.internal.multipart;

import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static io.datareplication.internal.multipart.Combinators.Pos;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CombinatorsTest {
    private static ByteBuffer utf8(String s) {
        return ByteBuffer.wrap(s.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void tag_shouldMatchTag() {
        Optional<Pos> result = Combinators.tag(utf8(" .tag. ")).parse(utf8(" .tag. and some more stuff"), 0);

        assertThat(result).contains(new Pos(0, 7));
    }

    @Test
    void tag_shouldMatchTagWithNonzeroStart() {
        Optional<Pos> result = Combinators.tag(utf8("tag")).parse(utf8("stuff that's ignored, then tag"), 27);

        assertThat(result).contains(new Pos(27, 30));
    }

    @Test
    void tag_shouldNotMatchAtAll() {
        Optional<Pos> result = Combinators.tag(utf8("word")).parse(utf8("whatevs, also word"), 0);

        assertThat(result).isEmpty();
    }

    @Test
    void tag_shouldNotMatchLate() {
        Optional<Pos> result = Combinators.tag(utf8("horse")).parse(utf8("horsing around"), 0);

        assertThat(result).isEmpty();
    }

    @Test
    void tag_shouldRequestMoreInput() {
        assertThatThrownBy(() -> Combinators.tag(utf8("horse")).parse(utf8("hors"), 0))
                .isInstanceOf(RequestInput.class);
    }

    @Test
    void tag_shouldRequestMoreInputWithNonzeroStart() {
        assertThatThrownBy(() -> Combinators.tag(utf8("horse")).parse(utf8("prefix, and: hors"), 13))
                .isInstanceOf(RequestInput.class);
    }

    @Test
    void eol_shouldMatchLF() {
        Optional<Pos> result = Combinators.eol().parse(utf8("\n"), 0);

        assertThat(result).contains(new Pos(0, 1));
    }

    @Test
    void eol_shouldMatchLFWithNonzeroStart() {
        Optional<Pos> result = Combinators.eol().parse(utf8("word\nand word"), 4);

        assertThat(result).contains(new Pos(4, 5));
    }

    @Test
    void eol_shouldMatchCRLF() {
        Optional<Pos> result = Combinators.eol().parse(utf8("\r\n"), 0);

        assertThat(result).contains(new Pos(0, 2));
    }

    @Test
    void eol_shouldMatchCRLFWithNonzeroStart() {
        Optional<Pos> result = Combinators.eol().parse(utf8("prefix\r\nsuffix"), 6);

        assertThat(result).contains(new Pos(6, 8));
    }

    @Test
    void eol_shouldNotMatchCR() {
        Optional<Pos> result = Combinators.eol().parse(utf8("\r and stuff"), 0);

        assertThat(result).isEmpty();
    }

    @Test
    void eol_shouldNotMatch() {
        Optional<Pos> result = Combinators.eol().parse(utf8("some stuff first: \n \r\n"), 0);

        assertThat(result).isEmpty();
    }

    @Test
    void eol_shouldRequestMoreInputForCR() {
        assertThatThrownBy(() -> Combinators.eol().parse(utf8("\r"), 0))
                .isInstanceOf(RequestInput.class);
    }

    @Test
    void scan_shouldFindParserAtStart() {
        Optional<Pos> result = Combinators.scan(Combinators.tag(utf8("test")))
                .parse(utf8("test"), 0);

        assertThat(result).contains(new Pos(0, 4));
    }

    @Test
    void scan_shouldFindParserAtStartWithNonzeroStart() {
        Optional<Pos> result = Combinators.scan(Combinators.tag(utf8("test")))
                .parse(utf8("123456: test"), 8);

        assertThat(result).contains(new Pos(8, 12));
    }

    @Test
    void scan_shouldFindParser() {
        Optional<Pos> result = Combinators.scan(Combinators.tag(utf8("test")))
                .parse(utf8("first some stuff, and then test"), 0);

        assertThat(result).contains(new Pos(27, 31));
    }

    @Test
    void scan_shouldFirstResultOfParser() {
        Optional<Pos> result = Combinators.scan(Combinators.tag(utf8("test")))
                .parse(utf8("a test b test c test test d"), 0);

        assertThat(result).contains(new Pos(2, 6));
    }

    @Test
    void scan_shouldNotMatch() {
        Optional<Pos> result = Combinators.scan(Combinators.tag(utf8("test")))
                .parse(utf8("tessellation"), 0);

        assertThat(result).isEmpty();
    }

    @Test
    void scan_shouldRequestMoreInput() {
        assertThatThrownBy(() -> Combinators.scan(Combinators.tag(utf8("test")))
                .parse(utf8("some prefix stuff, and then tes"), 0))
                .isInstanceOf(RequestInput.class);
    }

    @Test
    void seq_shouldMatch() {
        Optional<Pos> result = Combinators.seq(Combinators.tag(utf8("word")), Combinators.eol())
                .parse(utf8("word\r\n"), 0);

        assertThat(result).contains(new Pos(0, 6));
    }

    @Test
    void seq_shouldMatchWithNonzeroStart() {
        Optional<Pos> result = Combinators.seq(Combinators.tag(utf8("word")), Combinators.eol())
                .parse(utf8("ignored: word\r\n"), 9);

        assertThat(result).contains(new Pos(9, 15));
    }

    @Test
    void seq_shouldNotMatchIfFirstIsNotMatching() {
        Optional<Pos> result = Combinators.seq(Combinators.tag(utf8("word")), Combinators.eol())
                .parse(utf8("woah\n"), 0);

        assertThat(result).isEmpty();
    }

    @Test
    void seq_shouldNotMatchIfSecondIsNotMatching() {
        Optional<Pos> result = Combinators.seq(Combinators.tag(utf8("word")), Combinators.eol())
                .parse(utf8("word..."), 0);

        assertThat(result).isEmpty();
    }

    @Test
    void seq_shouldRequestMoreInputForFirst() {
        assertThatThrownBy(() -> Combinators.seq(Combinators.tag(utf8("test")), Combinators.eol())
                .parse(utf8("tes"), 0))
                .isInstanceOf(RequestInput.class);
    }

    @Test
    void seq_shouldRequestMoreInputForSecond() {
        assertThatThrownBy(() -> Combinators.seq(Combinators.tag(utf8("test")), Combinators.eol())
                .parse(utf8("test\r"), 0))
                .isInstanceOf(RequestInput.class);
    }

    @Test
    void seq_shouldRequestMoreInputIfOnlyFirstInInput() {
        assertThatThrownBy(() -> Combinators.seq(Combinators.tag(utf8("test")), Combinators.eol())
                .parse(utf8("test"), 0))
                .isInstanceOf(RequestInput.class);
    }

    @Test
    void either_shouldMatchFirst() {
        Optional<Pos> result = Combinators.either(Combinators.tag(utf8("a")),
                                                  Combinators.tag(utf8("b")))
                .parse(utf8("a"), 0);

        assertThat(result).contains(new Pos(0, 1));
    }

    @Test
    void either_shouldMatchSecond() {
        Optional<Pos> result = Combinators.either(Combinators.tag(utf8("a")),
                                                  Combinators.tag(utf8("b")))
                .parse(utf8("b"), 0);

        assertThat(result).contains(new Pos(0, 1));
    }

    @Test
    void either_shouldMatchFirstWithNonzeroStart() {
        Optional<Pos> result = Combinators.either(Combinators.tag(utf8("wordA")),
                                                  Combinators.tag(utf8("wordB")))
                .parse(utf8("test wordA stuff"), 5);

        assertThat(result).contains(new Pos(5, 10));
    }

    @Test
    void either_shouldMatchSecondWithNonzeroStart() {
        Optional<Pos> result = Combinators.either(Combinators.tag(utf8("wordA")),
                                                  Combinators.tag(utf8("wordB")))
                .parse(utf8("test wordB more"), 5);

        assertThat(result).contains(new Pos(5, 10));
    }

    @Test
    void either_shouldNotMatch() {
        Optional<Pos> result = Combinators.either(Combinators.tag(utf8("wordA")),
                                                  Combinators.tag(utf8("wordB")))
                .parse(utf8("wordC"), 0);

        assertThat(result).isEmpty();
    }

    @Test
    void either_shouldRequestMoreInputForFirst() {
        assertThatThrownBy(() -> Combinators.either(Combinators.tag(utf8("word")),
                                                    Combinators.tag(utf8("deed")))
                .parse(utf8("wor"), 0))
                .isInstanceOf(RequestInput.class);
    }

    @Test
    void either_shouldRequestMoreInputForSecond() {
        assertThatThrownBy(() -> Combinators.either(Combinators.tag(utf8("word")),
                                                    Combinators.tag(utf8("deed")))
                .parse(utf8("de"), 0))
                .isInstanceOf(RequestInput.class);
    }

    @Test
    void either_shouldMatchIfFirstIsPrefix() {
        Optional<Pos> result = Combinators.either(Combinators.tag(utf8("pre")),
                                                  Combinators.tag(utf8("prefix")))
                .parse(utf8("prefix"), 0);

        assertThat(result).contains(new Pos(0, 3));
    }

    @Test
    void either_shouldRequestMoreInputIfSecondIsPrefix() {
        assertThatThrownBy(() -> Combinators.either(Combinators.tag(utf8("prefix")),
                                                    Combinators.tag(utf8("pre")))
                .parse(utf8("pre"), 0))
                .isInstanceOf(RequestInput.class);
    }
}
