package io.datareplication.model;

import io.datareplication.internal.multipart.MultipartUtils;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.ToString;
import lombok.Value;

import java.io.InputStream;
import java.io.SequenceInputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

/**
 * The <code>Page</code> class represents a complete feed or snapshot page, both on the producer and consumer side. This
 * class always represents a page that is fully present in memory; it does not handle streaming.
 *
 * @param <PageHeader>   the type of the page header; in practice this will be either
 *                       * {@link io.datareplication.model.snapshot.SnapshotPageHeader} or
 *                       * {@link io.datareplication.model.feed.FeedPageHeader}
 * @param <EntityHeader> the type of the entity headers; see {@link Entity}
 */
@Value
public class Page<PageHeader extends ToHttpHeaders, EntityHeader extends ToHttpHeaders> {
    private static final Body CRLF = Body.fromUtf8("\r\n");

    /**
     * The page's headers. This does not include Content-Length and Content-Type which are included in the return value
     * of {@link #toMultipartBody()}.
     */
    @NonNull PageHeader header;
    /**
     * The boundary string for the page's multipart representation.
     */
    @NonNull String boundary;
    /**
     * The list of entities for this page.
     */
    @NonNull List<@NonNull Entity<@NonNull EntityHeader>> entities;

    public Page(@NonNull PageHeader header,
                @NonNull String boundary,
                @NonNull List<@NonNull Entity<@NonNull EntityHeader>> entities) {
        this.header = header;
        this.boundary = boundary;
        this.entities = List.copyOf(entities);
    }

    /**
     * <p>Return the body of this page as a multipart document.</p>
     *
     * <p>
     * This function does not allocate a buffer for the entire page body. Instead, the entities' bodies
     * are directly reused by the returned {@link Body} to avoid having to keep entity bodies in memory twice.
     * </p>
     *
     * <p>
     * The returned body is designed to serve the page over HTTP. The returned {@link Body}'s headers <em>must</em> also
     * by served as part of the HTTP header for the page to be consumable.
     * </p>
     *
     * @return a Body containing the page's entities as a multipart document
     */
    public @NonNull Body toMultipartBody() {
        final var chunks = new ArrayList<Body>();
        for (var entity : entities) {
            final var partHeader = new StringBuilder(100)
                .append("--")
                .append(boundary)
                .append("\r\n");
            for (var header : entity.toHttpHeaders()) {
                for (var value : header.values()) {
                    partHeader
                        .append(header.displayName())
                        .append(": ")
                        .append(value)
                        .append("\r\n");
                }
            }
            partHeader.append("\r\n");
            chunks.add(Body.fromUtf8(partHeader.toString()));
            chunks.add(entity.body());
            chunks.add(CRLF);
        }

        chunks.add(Body.fromUtf8(String.format("--%s--", boundary)));
        return new MultipartBody(MultipartUtils.pageContentType(boundary), chunks);
    }

    @EqualsAndHashCode
    @ToString
    @AllArgsConstructor
    private static final class MultipartBody implements Body {
        private final ContentType contentType;
        private final List<Body> bodyParts;

        @Override
        public @NonNull InputStream newInputStream() {
            final var inputStreamsIterator = bodyParts
                .stream()
                .map(Body::newInputStream)
                .iterator();
            final var inputStreamsEnumeration = new Enumeration<InputStream>() {
                @Override
                public boolean hasMoreElements() {
                    return inputStreamsIterator.hasNext();
                }

                @Override
                public InputStream nextElement() {
                    return inputStreamsIterator.next();
                }
            };
            return new SequenceInputStream(inputStreamsEnumeration);
        }

        @Override
        public long contentLength() {
            return bodyParts.stream().mapToLong(Body::contentLength).sum();
        }

        @Override
        public @NonNull ContentType contentType() {
            return contentType;
        }
    }
}
