package io.datareplication.internal.http;

import io.datareplication.consumer.PageFormatException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MultipartContentTypeTest {
    private static Stream<Arguments> validContentTypes() {
        return Stream.of(
            Arguments.of(
                "multipart/mixed; boundary=_---_valid-header",
                new MultipartContentType("multipart/mixed", "_---_valid-header")),
            Arguments.of(
                "multipart/blub; boundary=abcd",
                new MultipartContentType("multipart/blub", "abcd")),
            Arguments.of(
                "   multipart/mixed    ;      boundary=test boundary  ",
                new MultipartContentType("multipart/mixed", "test boundary")),
            Arguments.of(
                "MULTIPART/PACKAGE; BOUNDARY=FOOBAR",
                new MultipartContentType("MULTIPART/PACKAGE", "FOOBAR")),
            Arguments.of(
                "Multipart; bounDArY = \"---;==;---\"",
                new MultipartContentType("Multipart", "---;==;---"))
        );
    }

    private static Stream<Arguments> invalidContentTypes() {
        return Stream.of(
            Arguments.of(
                "text/plain",
                new PageFormatException.InvalidContentType("text/plain")),
            Arguments.of(
                "text/plain; boundary=nope",
                new PageFormatException.InvalidContentType("text/plain")),
            Arguments.of(
                "multipart/mixed",
                new PageFormatException.NoBoundaryInContentTypeHeader("multipart/mixed")),
            Arguments.of(
                "multipart/mixed; boundary=\"unterminated",
                new PageFormatException.UnparseableContentTypeHeader("multipart/mixed; boundary=\"unterminated"))
        );
    }

    @ParameterizedTest
    @MethodSource("validContentTypes")
    void shouldParseValidMultipartContentTypes(String input, MultipartContentType expected) {
        assertThat(MultipartContentType.parse(input)).isEqualTo(expected);
    }

    @ParameterizedTest
    @MethodSource("invalidContentTypes")
    void shouldNotParseInvalidMultipartContentTypes(String input, Throwable expected) {
        assertThatThrownBy(() -> MultipartContentType.parse(input)).isEqualTo(expected);
    }
}
