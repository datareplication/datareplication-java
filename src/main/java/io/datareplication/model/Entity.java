package io.datareplication.model;

import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.Value;

import java.util.Optional;

/**
 * The <code>Entity</code> class represents a feed or snapshot entity. It consists of entity headers, a body, and an
 * optional piece of user-specified metadata.
 *
 * @param <Header> the type of the entity headers; in practice this will be either
 * {@link io.datareplication.model.snapshot.SnapshotEntityHeader} or
 * {@link io.datareplication.model.feed.FeedEntityHeader}
 */
@Value
@AllArgsConstructor
public class Entity<Header extends ToHttpHeaders> implements ToHttpHeaders {
    /**
     * The entity's headers. This does not include Content-Type and Content-Length which are associated with
     * {@link #body()}.
     */
    @NonNull Header header;
    /**
     * The entity's body. This includes the actual bytes as well as the Content-Type and Content-Length.
     */
    @NonNull Body body;
    /**
     * <p>
     * An optional piece of user-specified metadata. The library implementation never touches or interprets it. It
     * exists exclusively to allow library users to carry additional information with an entity as it passes through the
     * library. This means that it is up to the library user how to handle this field at boundaries (e.g. when storing
     * entities in a database): it's not necessary to store and load this field if you don't ever use it.
     * </p>
     *
     * <p>
     * This field is an internal aspect of the library intended for producers: it is not part of the data format and
     * won't be included in a consumable snapshot or feed. When consuming entities from a feed or snapshot, this field
     * will always be empty.
     * </p>
     */
    @NonNull Optional<@NonNull Object> userData;

    /**
     * Create a new entity with empty <code>userData</code>.
     *
     * @param header the entity header
     * @param body the entity body
     */
    public Entity(@NonNull Header header, @NonNull Body body) {
        this(header, body, Optional.empty());
    }

    /**
     * Return all headers for this entity. This combines the actual entity headers with the Content-Type and
     * Content-Length headers from the {@link #body()}.
     *
     * @return all headers for this entity
     */
    @Override
    public @NonNull HttpHeaders toHttpHeaders() {
        return header.toHttpHeaders().update(body.toHttpHeaders());
    }
}
