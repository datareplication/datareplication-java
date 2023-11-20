package io.datareplication.model;

import lombok.NonNull;
import lombok.Value;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
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

    private static byte[] readAll(InputStream input) throws IOException {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            input.transferTo(output);
            return output.toByteArray();
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

    @Test
    void fromBytes_shouldReturnBody() throws IOException {
        final byte[] bytes = new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10};

        final Body body = Body.fromBytes(bytes, ContentType.of("application/xml"));

        assertThat(readAll(body.newInputStream())).isEqualTo(bytes);
        assertThat(body.contentLength()).isEqualTo(10);
        assertThat(body.contentType()).isEqualTo(ContentType.of("application/xml"));
    }

    @Test
    void fromBytes_shouldReturnBodyWithDefaultContentType() throws IOException {
        final byte[] bytes = new byte[]{6, 6, 6, 6, 6, 6};

        final Body body = Body.fromBytes(bytes);

        assertThat(readAll(body.newInputStream())).isEqualTo(bytes);
        assertThat(body.contentLength()).isEqualTo(6);
        assertThat(body.contentType()).isEqualTo(ContentType.of("application/octet-stream"));
    }

    @Test
    void fromBytes_shouldNotReturnTheSameByteArrayFromToBytes() throws IOException {
        final byte[] bytes = new byte[]{1, 2, 3, 5, 8, 13, 21};

        final Body body = Body.fromBytes(bytes);

        assertThat(body.toBytes()).isEqualTo(bytes);
        assertThat(body.toBytes()).isNotSameAs(bytes);
    }

    @Test
    void fromBytes_shouldHaveSameEqualToAndHashCode() {
        final byte[] bytes1 = new byte[]{1, 2, 3, 5, 8, 13, 21};
        final byte[] bytes2 = bytes1.clone();

        final Body body1 = Body.fromBytes(bytes1);
        final Body body2 = Body.fromBytes(bytes2);

        assertThat(body1).isEqualTo(body2);
        assertThat(body1).hasSameHashCodeAs(body2);
    }

    @Test
    void fromBytes_shouldCopyArray() throws IOException {
        final var bytes = new byte[]{1, 2, 3, 4};
        final var expected = bytes.clone();

        final Body body = Body.fromBytes(bytes);
        bytes[0] = 50;

        assertThat(body.toBytes()).isEqualTo(expected);
    }

    @Test
    void fromBytesUnsafe_shouldNotCopyArray() throws IOException {
        final var bytes = new byte[]{1, 2, 3, 4};
        final var expected = bytes.clone();

        final Body body = Body.fromBytesUnsafe(bytes);
        bytes[0] = 50;

        assertThat(body.toBytes()).isEqualTo(bytes);
        assertThat(body.toBytes()).isNotEqualTo(expected);
    }

    @Test
    void fromUtf8_shouldReturnBody_whenAsciiOnly() throws IOException {
        final String s = "this is the test string";

        final Body body = Body.fromUtf8(s, ContentType.of("text/html"));

        assertThat(readAll(body.newInputStream())).isEqualTo(s.getBytes(StandardCharsets.US_ASCII));
        assertThat(body.contentLength()).isEqualTo(s.length());
        assertThat(body.contentType()).isEqualTo(ContentType.of("text/html"));
    }

    @Test
    void fromUtf8_shouldReturnBodyWithDefaultContentType() throws IOException {
        final String s = "this is the test string";

        final Body body = Body.fromUtf8(s);

        assertThat(readAll(body.newInputStream())).isEqualTo(s.getBytes(StandardCharsets.US_ASCII));
        assertThat(body.contentLength()).isEqualTo(s.length());
        assertThat(body.contentType()).isEqualTo(ContentType.of("text/plain; charset=utf-8"));
    }

    @Test
    void fromUtf8_shouldReturnBody_whenNotAsciiOnly() throws IOException {
        final String s = "test äöüß é à";

        final Body body = Body.fromUtf8(s);

        assertThat(readAll(body.newInputStream())).isEqualTo(s.getBytes(StandardCharsets.UTF_8));
        assertThat(body.contentLength()).isEqualTo(19);
    }

    @Test
    void fromUtf8_shouldReturnTheSameStringFromToUtf8() throws IOException {
        final String s = "test string";

        final Body body = Body.fromUtf8(s);

        assertThat(body.toUtf8()).isEqualTo(s);
        assertThat(body.toUtf8()).isSameAs(s);
    }

    @Test
    void fromUtf8_shouldHaveSameEqualToAndHashCode() {
        final String s = "you can compare these specific implementations because it's useful for tests";

        final Body body1 = Body.fromUtf8(s);
        final Body body2 = Body.fromUtf8(s);

        assertThat(body1).isEqualTo(body2);
        assertThat(body1).hasSameHashCodeAs(body2);
    }
}
