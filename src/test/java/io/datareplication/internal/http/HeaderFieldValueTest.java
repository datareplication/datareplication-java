package io.datareplication.internal.http;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Collections;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HeaderFieldValueTest {
    private static Stream<Arguments> validHeaderFieldValues() {
        return Stream.of(
            Arguments.of(
                "",
                new HeaderFieldValue("", Collections.emptyMap())),
            Arguments.of(
                "a",
                new HeaderFieldValue("a", Collections.emptyMap())),
            Arguments.of(
                "; a=b",
                new HeaderFieldValue("", Map.of("a", "b"))),
            Arguments.of(
                "a; b=c",
                new HeaderFieldValue("a", Map.of("b", "c"))),
            Arguments.of(
                "AbCDe; pArAM=VALUe",
                new HeaderFieldValue("AbCDe", Map.of("param", "VALUe"))),
            Arguments.of(
                "a; b=",
                new HeaderFieldValue("a", Map.of("b", ""))),
            Arguments.of(
                "a; b =    ",
                new HeaderFieldValue("a", Map.of("b", ""))),
            Arguments.of(
                "a; b=c; d=e",
                new HeaderFieldValue("a", Map.of("b", "c", "d", "e"))),
            Arguments.of(
                "a; b=\"\"",
                new HeaderFieldValue("a", Map.of("b", ""))),
            Arguments.of(
                "a;b=\"c\";d=e",
                new HeaderFieldValue("a", Map.of("b", "c", "d", "e"))),
            Arguments.of(
                "    a         ",
                new HeaderFieldValue("a", Collections.emptyMap())),
            Arguments.of(
                "   a  ;   b   =  \"  c  \"   ;   d  =     e",
                new HeaderFieldValue("a", Map.of("b", "  c  ", "d", "e"))),
            Arguments.of(
                "a; a=\"trap: ; c=d\"",
                new HeaderFieldValue("a", Map.of("a", "trap: ; c=d"))),
            Arguments.of(
                "<some other stuff/abcd\">; a=\"ab\\\"cd\"",
                new HeaderFieldValue("<some other stuff/abcd\">", Map.of("a", "ab\"cd"))),
            Arguments.of(
                "abcd; p=\"ab\\ \\\\c\\\"';;;\"",
                new HeaderFieldValue("abcd", Map.of("p", "ab \\c\"';;;"))),
            Arguments.of(
                "a;=d",
                new HeaderFieldValue("a", Map.of("", "d"))),
            Arguments.of(
                "a;= d  ",
                new HeaderFieldValue("a", Map.of("", "d")))
        );
    }

    private static Stream<Arguments> invalidHeaderFieldValues() {
        return Stream.of(
            Arguments.of("a;"),
            Arguments.of("a; b=c;"),
            Arguments.of("a; b=c;; d=e"),
            Arguments.of("a; b"),
            Arguments.of("a; b; c=d"),
            Arguments.of("a; b=c;d"),
            Arguments.of("a; b=\"unbalanced"),
            Arguments.of("a; b=\"unbalanced; c=d")
        );
    }

    @ParameterizedTest
    @MethodSource("validHeaderFieldValues")
    void shouldParseValidHeaderFieldValues(String input, HeaderFieldValue expected) {
        assertThat(HeaderFieldValue.parse(input)).isEqualTo(expected);
    }

    @ParameterizedTest
    @MethodSource("invalidHeaderFieldValues")
    void shouldNotParseInvalidHeaderFieldValues(String input) {
        assertThatThrownBy(() -> HeaderFieldValue.parse(input)).isInstanceOf(IllegalArgumentException.class);
    }
}
