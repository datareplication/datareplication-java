package io.datareplication.model;

import lombok.NonNull;
import lombok.Value;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BodyTest {
    private static final ContentType ANY_CONTENT_TYPE = ContentType.of("application/octet-stream");

    @Value
    private static class TestBody implements Body {
        Supplier<InputStream> inputStreamSupplier;
        long contentLength;
        ContentType contentType;

        @Override
        public @NonNull InputStream newInputStream() {
            return inputStreamSupplier.get();
        }

        @Override
        public long contentLength() {
            return contentLength;
        }

        @Override
        public @NonNull ContentType contentType() {
            return contentType;
        }
    }

    private static final class EmptyInputStream extends InputStream {
        @Override
        public int read() {
            return -1;
        }
    }

    private static final class ThrowingInputStream extends InputStream {

        @Override
        public int read() throws IOException {
            throw new IOException("test");
        }
    }

    @Test
    void toHttpHeaders_shouldReturnContentLengthAndContentTypeHeaders() {
        final TestBody body = new TestBody(EmptyInputStream::new, 591, ContentType.of("application/json"));

        final HttpHeaders result = body.toHttpHeaders();

        assertThat(result).isEqualTo(HttpHeaders.of(
            HttpHeader.contentLength(591),
            HttpHeader.contentType(ContentType.of("application/json"))
        ));
    }

    @Test
    void toHttpHeaders_shouldReturnContentLength_whenContentLengthIsZero() {
        final TestBody body = new TestBody(EmptyInputStream::new, 0, ANY_CONTENT_TYPE);

        final HttpHeaders result = body.toHttpHeaders();

        assertThat(result).isEqualTo(HttpHeaders.of(
            HttpHeader.contentLength(0),
            HttpHeader.contentType(ANY_CONTENT_TYPE)
        ));
    }

    @Test
    void toHttpHeaders_shouldReturnContentLength_whenContentLengthIsNegative() {
        final TestBody body = new TestBody(EmptyInputStream::new, -1, ANY_CONTENT_TYPE);

        final HttpHeaders result = body.toHttpHeaders();

        assertThat(result).isEqualTo(HttpHeaders.of(
            HttpHeader.contentLength(-1),
            HttpHeader.contentType(ANY_CONTENT_TYPE)
        ));
    }

    @Test
    void toUtf8_shouldDecodeUtf8Bytes() throws IOException {
        final String s = "some utf-8 string: äöüéèâ€ \uD83D\uDE43";
        final TestBody body = new TestBody(() -> new ByteArrayInputStream(s.getBytes(StandardCharsets.UTF_8)),
                                           2,
                                           ANY_CONTENT_TYPE);

        final String result = body.toUtf8();

        assertThat(result).isEqualTo(s);
    }

    @Test
    void toUtf8_shouldDecodeUtf8Bytes_whenContentLengthIsNegative() throws IOException {
        final String s = "abcd";
        final TestBody body = new TestBody(() -> new ByteArrayInputStream(s.getBytes(StandardCharsets.UTF_8)),
                                           -4,
                                           ANY_CONTENT_TYPE);

        final String result = body.toUtf8();

        assertThat(result).isEqualTo(s);
    }

    @Test
    void toUtf8_shouldDecodeEmptyInputStream() throws IOException {
        final TestBody body = new TestBody(EmptyInputStream::new, 0, ANY_CONTENT_TYPE);

        final String result = body.toUtf8();

        assertThat(result).isEqualTo("");
    }

    @Test
    void toUtf8_shouldPassThroughIOException() {
        final TestBody body = new TestBody(ThrowingInputStream::new, -1, ANY_CONTENT_TYPE);

        assertThatThrownBy(body::toUtf8).isInstanceOf(IOException.class);
    }

    @Test
    void toUtf8_shouldThrowCharacterCodingException_whenInvalidUtf8() {
        final byte[] bytes = new byte[]{-61, 40};
        final TestBody body = new TestBody(() -> new ByteArrayInputStream(bytes), 2, ANY_CONTENT_TYPE);

        assertThatThrownBy(body::toUtf8).isInstanceOf(CharacterCodingException.class);
    }

    @Test
    void toUtf8_shouldThrowCharacterCodingException_whenSingleUtf16Surrogate() {
        final byte[] bytes = new byte[]{-19, -96, -128};
        final TestBody body = new TestBody(() -> new ByteArrayInputStream(bytes), 3, ANY_CONTENT_TYPE);

        assertThatThrownBy(body::toUtf8).isInstanceOf(CharacterCodingException.class);
    }

    @Test
    void toUtf8_shouldThrowCharacterCodingException_whenPairedUtf16Surrogates() {
        final byte[] bytes = new byte[]{-19, -96, -19, -80, -128};
        final TestBody body = new TestBody(() -> new ByteArrayInputStream(bytes), 3, ANY_CONTENT_TYPE);

        assertThatThrownBy(body::toUtf8).isInstanceOf(CharacterCodingException.class);
    }

    @Test
    void toBytes_shouldReturnBytes() throws IOException {
        final byte[] bytes = new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
        final TestBody body = new TestBody(() -> new ByteArrayInputStream(bytes), 10, ANY_CONTENT_TYPE);

        final byte[] result = body.toBytes();

        assertThat(result).isEqualTo(bytes);
    }

    @Test
    void toBytes_shouldReturnBytes_whenContentLengthIsNegative() throws IOException {
        final byte[] bytes = new byte[]{1, 2};
        final TestBody body = new TestBody(() -> new ByteArrayInputStream(bytes), -2, ANY_CONTENT_TYPE);

        final byte[] result = body.toBytes();

        assertThat(result).isEqualTo(bytes);
    }

    @Test
    void toBytes_shouldReturnBytesOfEmptyInputStream() throws IOException {
        final TestBody body = new TestBody(EmptyInputStream::new, 0, ANY_CONTENT_TYPE);

        final byte[] result = body.toBytes();

        assertThat(result).isEmpty();
    }

    @Test
    void toBytes_shouldPassThroughIOException() {
        final TestBody body = new TestBody(ThrowingInputStream::new, 0, ANY_CONTENT_TYPE);

        assertThatThrownBy(body::toBytes).isInstanceOf(IOException.class);
    }
}
