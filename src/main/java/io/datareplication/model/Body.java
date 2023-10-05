package io.datareplication.model;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.ToString;
import org.apache.commons.io.input.ReaderInputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;

/**
 * <p>
 * The <code>Body</code> interface represents a sequence of bytes (usually for an entity or page body) including a
 * length and an HTTP content type.
 * </p>
 *
 * <p>
 * The central method is {@link #newInputStream()} which must return a new {@link java.io.InputStream} over the
 * underlying byte sequence. Each call must create and return a new InputStream. Simply creating and returning the same
 * InputStream each time is not sufficient! Think of it like the {@link Iterable#iterator()} method in a collection:
 * each iterator() call creates a new iterator with its own internal iteration state, but they all provide a view of the
 * same underlying collection. Similarly, each call to {@link #newInputStream()} creates a new InputStream providing a
 * view of the underlying bytes.
 * This implies that this interface is not suitable for streaming data from e.g. a network connection on demand. The
 * underlying bytes must be available so they can be iterated over multiple times.
 * </p>
 *
 * <p>
 * The motivation for this interface is to provide a way to handle sequences of bytes while imposing as few restrictions
 * on their internal representation as possible. By only providing sequential access, the underlying bytes can be stored
 * as a byte array, a {@link String}, or in compressed form. They can even be concatenated from multiple internal
 * representations on the fly. This flexibility makes it possible to handle large byte sequences while minimizing the
 * number of full copies that have to be made and kept in memory.
 * </p>
 */
public interface Body extends ToHttpHeaders {
    /**
     * <p>
     * Create a new {@link java.io.InputStream} pointing to the start of the underlying byte sequence.
     * </p>
     *
     * <p>
     * InputStreams created by this method must always read the same bytes as every other InputStream created by this
     * method, but their internal read states must be completely independent.
     * </p>
     *
     * @return a new InputStream over the underlying bytes
     */
    @NonNull InputStream newInputStream();

    /**
     * Return the length of the underlying byte sequence in bytes. Must not be negative.
     *
     * @return the length of the underlying byte sequence
     */
    long contentLength();

    // TODO: Putting the content type on the Body is useful in multiple places, but causes issues when you want all
    //       headers for a given thing but not the body. That can be solved in those places by having those headers as
    //       extra fields there, but maybe not having the extra headers in here is a better design. Alternatively: put
    //       content length and type in an extra class (BodyHeaders?) and return that here.

    /**
     * Return the content type of the underlying data. This should be a MIME type suitable for use in a
     * <code>Content-Type</code> header.
     *
     * @return the content type of the data
     */
    @NonNull ContentType contentType();

    /**
     * <p>
     * Return the <code>Content-Length</code> and <code>Content-Type</code> headers for this Body.
     * </p>
     *
     * <p>
     * Overriding this method is not recommended.
     * </p>
     *
     * @return {@link HttpHeaders} containing the length and type headers for this Body
     */
    @Override
    default @NonNull HttpHeaders toHttpHeaders() {
        return HttpHeaders.of(
            HttpHeader.contentType(contentType()),
            HttpHeader.contentLength(contentLength())
        );
    }

    /**
     * <p>
     * Decode this byte sequence as UTF-8.
     * </p>
     *
     * <p>
     * The default implementation decodes the bytes and returns the result. You can choose to override it with a more
     * efficient implementation if your presentation allows.
     * </p>
     *
     * @return the content of this Body as UTF-8
     * @throws IOException              when the InputStream returned by {{@link #newInputStream()}} throws IOException
     * @throws CharacterCodingException when the underlying bytes are not valid UTF-8
     */
    default @NonNull String toUtf8() throws IOException {
        try (StringWriter writer = new StringWriter(getBufferSize(this))) {
            try (InputStream input = newInputStream()) {
                final CharsetDecoder decoder = StandardCharsets.UTF_8
                    .newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT);
                try (InputStreamReader wrappingReader = new InputStreamReader(input, decoder)) {
                    wrappingReader.transferTo(writer);
                }
            }
            return writer.toString();
        }
    }

    /**
     * <p>
     * Return this byte sequence as a byte array.
     * </p>
     *
     * <p>
     * The default implementation fills the array incrementally. You can choose to override it with a more efficient
     * implementation, but take care not to leak mutable access to your internal data by returning a reference to a
     * byte array that's stored in your class.
     * </p>
     *
     * @return the byte contents of this Body
     * @throws IOException when the InputStream returned by {{@link #newInputStream()}} throws an IOException
     */
    default @NonNull byte[] toBytes() throws IOException {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream(getBufferSize(this))) {
            try (InputStream input = newInputStream()) {
                input.transferTo(output);
            }
            return output.toByteArray();
        }
    }

    private static int getBufferSize(Body body) {
        return Math.max((int) body.contentLength(), 0);
    }

    /**
     * Return a Body over the bytes of the given String encoded as UTF-8. This method retains the String and does not
     * allocate an additional backing buffer.
     *
     * @param utf8        the String
     * @param contentType the content type for the created Body
     * @return a Body of the bytes of the given String encoded as UTF-8
     */
    static @NonNull Body fromUtf8(@NonNull String utf8, @NonNull ContentType contentType) {
        @EqualsAndHashCode
        @ToString
        @AllArgsConstructor
        class Utf8Body implements Body {
            private final String utf8;
            private final long contentLength;
            private final ContentType contentType;

            @Override
            public @NonNull InputStream newInputStream() {
                try {
                    // TODO: maybe replace this to get rid of commons-io dependency
                    return ReaderInputStream
                        .builder()
                        .setCharset(StandardCharsets.UTF_8)
                        .setReader(new StringReader(utf8))
                        .get();
                } catch (IOException e) {
                    // I'm assuming this can't happen, based on reading the source and the parameters we supply
                    throw new IllegalStateException("unexpected IOException in fromUtf8#newInputStream; bug?", e);
                }
            }

            @Override
            public long contentLength() {
                return contentLength;
            }

            @Override
            public @NonNull ContentType contentType() {
                return contentType;
            }

            @Override
            public @NonNull String toUtf8() {
                return utf8;
            }
        }
        return new Utf8Body(utf8, countUtf8Bytes(utf8), contentType);
    }

    /**
     * Return a Body over the bytes of the given String encoded as UTF-8 with the default content type
     * <code>text/plain; charset=utf-8</code>.
     *
     * @param utf8 the String
     * @return a Body of the bytes of the given String encoded as UTF-8
     * @see #fromUtf8(String, ContentType)
     */
    static @NonNull Body fromUtf8(@NonNull String utf8) {
        return fromUtf8(utf8, ContentType.of("text/plain; charset=utf-8"));
    }

    /**
     * <p>Return a Body containing the bytes of the given byte array.</p>
     *
     * <p>The byte array is not copied, therefore you have to make sure that the array is
     * <strong>NEVER MODIFIED</strong> after being passed to this method. If you can't guarantee that, always clone
     * the array before passing it to this method.
     * </p>
     *
     * @param bytes       the byte array
     * @param contentType the content type for the created Body
     * @return a Body of the array's bytes
     */
    static @NonNull Body fromBytes(@NonNull byte[] bytes, @NonNull ContentType contentType) {
        @EqualsAndHashCode
        @ToString
        @AllArgsConstructor
        class BytesBody implements Body {
            private final byte[] bytes;
            private final ContentType contentType;

            @Override
            public @NonNull InputStream newInputStream() {
                return new ByteArrayInputStream(bytes);
            }

            @Override
            public long contentLength() {
                return bytes.length;
            }

            @Override
            public @NonNull ContentType contentType() {
                return contentType;
            }

            @Override
            public @NonNull byte[] toBytes() {
                return bytes.clone();
            }
        }
        return new BytesBody(bytes, contentType);
    }

    /**
     * <p>Return a Body containing the bytes of the given byte array with the default content-type
     * <code>application/octet-stream</code>
     * </p>
     *
     * <p>The byte array is not copied, therefore you have to make sure that the array is
     * <strong>NEVER MODIFIED</strong> after being passed to this method. If you can't guarantee that, always clone
     * the array before passing it to this method.
     * </p>
     *
     * @param bytes the byte array
     * @return a Body of the array's bytes
     */
    static @NonNull Body fromBytes(@NonNull byte[] bytes) {
        return fromBytes(bytes, ContentType.of("application/octet-stream"));
    }

    private static long countUtf8Bytes(String utf8) {
        class CountingOutputStream extends OutputStream {
            private long count;

            @Override
            public void write(final byte[] b) {
                count += b.length;
            }

            @Override
            public void write(final byte[] b, final int off, final int len) {
                count += len;
            }

            @Override
            public void write(final int b) {
                count += 1;
            }
        }
        try (CountingOutputStream counter = new CountingOutputStream()) {
            try (OutputStreamWriter encoder = new OutputStreamWriter(counter, StandardCharsets.UTF_8)) {
                encoder.write(utf8);
            }
            return counter.count;
        } catch (IOException e) {
            // can't happen
            throw new IllegalStateException("unexpected IOException in countUtf8Bytes; bug?", e);
        }
    }
}
