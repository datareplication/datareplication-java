package io.datareplication.model;

import lombok.NonNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

/**
 * <p>
 * The <pre>Body</pre> interface represents a sequence of bytes (usually for an entity or page body) including a length
 * and an HTTP content type.
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
     * Return the length of the underlying byte sequence in bytes.
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
        ArrayList<HttpHeader> headers = new ArrayList<>();
        headers.add(HttpHeader.contentType(contentType()));
        long contentLength = contentLength();
        if (contentLength > 0) {
            headers.add(HttpHeader.contentLength(contentLength));
        }
        return HttpHeaders.of(headers);
    }

    // TODO: error handling; just throw and document a decoding exception I guess
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
     */
    default @NonNull String toUtf8() {
        throw new RuntimeException("not implemented");
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
     */
    default @NonNull byte[] toBytes() throws IOException {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream((int) contentLength())) {
            try (InputStream input = newInputStream()) {
                input.transferTo(output);
            }
            return output.toByteArray();
        }
    }

    static @NonNull Body fromUtf8(@NonNull String utf8) {
        return fromUtf8(utf8, ContentType.of("text/plain; charset=utf-8"));
    }

    static @NonNull Body fromUtf8(@NonNull String utf8, @NonNull ContentType contentType) {
        throw new RuntimeException("not implemented");
    }

    static @NonNull Body fromBytes(@NonNull byte[] bytes) {
        return fromBytes(bytes, ContentType.of("application/octet-stream"));
    }

    static @NonNull Body fromBytes(@NonNull byte[] bytes, @NonNull ContentType contentType) {
        return new Body() {
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
            public @NonNull byte[] toBytes() throws IOException {
                return bytes.clone();
            }
        };
    }
}
