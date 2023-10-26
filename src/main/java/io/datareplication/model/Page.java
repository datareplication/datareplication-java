package io.datareplication.model;

import lombok.NonNull;
import lombok.Value;

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
    /**
     * The page's headers. This does not include Content-Length and Content-Type which are included in the return value
     * of {@link #toMultipartBody()}.
     */
    @NonNull PageHeader header;
    /**
     * The list of entities for this page.
     */
    @NonNull List<@NonNull Entity<@NonNull EntityHeader>> entities;
    /**
     * The boundary string for the page's multipart representation.
     */
    @NonNull String boundary;

    public Page(@NonNull PageHeader header,
                @NonNull List<@NonNull Entity<@NonNull EntityHeader>> entities,
                @NonNull String boundary) {
        this.header = header;
        this.entities = List.copyOf(entities);
        this.boundary = String.format("_---_%s", boundary);
    }

    /**
     * <p>Return the body of this page as a multipart document.</p>
     *
     * <p>
     * This function does not allocate a buffer for the entire page body. Instead, it is generated on demand from the
     * underlying entities.
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
        throw new UnsupportedOperationException("not implemented");
    }
}
