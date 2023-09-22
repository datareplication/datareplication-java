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
    public void scan_shouldFindNeedle_equals() throws RequestInput {
        final Optional<Pos> result = Combinators.scan(utf8("some string"), 0, utf8("some string"));

        assertThat(result).contains(new Pos(0, 11));
    }

    @Test
    public void scan_shouldFindNeedle_startsWith() throws RequestInput {
        final Optional<Pos> result = Combinators.scan(utf8("some string and then more things"), 0, utf8("some str"));

        assertThat(result).contains(new Pos(0, 8));
    }

    @Test
    public void scan_shouldFindNeedle_endsWith() throws RequestInput {
        final Optional<Pos> result = Combinators.scan(utf8("some prefix and then"), 0, utf8(" and then"));

        assertThat(result).contains(new Pos(11, 20));
    }

    @Test
    public void scan_shouldFindNeedle_substring() throws RequestInput {
        final Optional<Pos> result = Combinators.scan(utf8("some prefix -needle- some suffix"), 0, utf8("-needle-"));

        assertThat(result).contains(new Pos(12, 20));
    }

    @Test
    public void scan_shouldFindNeedle_nonZeroStart() throws RequestInput {
        final Optional<Pos> result = Combinators.scan(utf8("-needle-, then other things, then -needle-, then a suffix"), 14, utf8("-needle-"));

        assertThat(result).contains(new Pos(34, 42));
    }

    @Test
    public void scan_shouldNotFindNeedle() throws RequestInput {
        final Optional<Pos> result = Combinators.scan(utf8("nope, but also a trap: -needle."), 0, utf8("-needle-"));

        assertThat(result).isEmpty();
    }

    @Test
    public void scan_shouldNotFindNeedle_nonZeroStart() throws RequestInput {
        final Optional<Pos> result = Combinators.scan(utf8("-needle-, other stuff, but not: -needl-"), 1, utf8("-needle-"));

        assertThat(result).isEmpty();
    }

    @Test
    public void scan_shouldRequestMoreInput() throws RequestInput {
        assertThatThrownBy(() -> Combinators.scan(utf8("-needle"), 0, utf8("-needle-")))
            .isInstanceOf(RequestInput.class);
    }

    @Test
    public void scan_shouldRequestMoreInput_shortInput() throws RequestInput {
        assertThatThrownBy(() -> Combinators.scan(utf8("prefix, and: t"), 0, utf8("teststring")))
            .isInstanceOf(RequestInput.class);
    }
}
