package io.datareplication.consumer;

import io.datareplication.internal.page.ToCompleteEntitiesTransformer;
import io.datareplication.model.ContentType;
import io.datareplication.model.Entity;
import io.datareplication.model.Page;
import io.datareplication.model.ToHttpHeaders;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.Value;
import org.reactivestreams.FlowAdapters;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;

/**
 * <p>
 * The <code>StreamingPage</code> interface represents a streaming feed or snapshot page. It allows consuming a page
 * incrementally without fully downloading it and keeping it in memory. By default, individual entity bodies are also
 * streamed in chunks. The size of each data chunk is implementation-defined and usually corresponds to the buffer size
 * used by the HTTP client implementation.
 * </p>
 *
 * <p>
 * <code>StreamingPage</code> extends the {@link Flow.Publisher} interface providing a stream of {@link Chunk}
 * objects. This is the lowest level of the interface and allows consuming entity bodies in small chunks. Alternatively,
 * the {@link #toCompleteEntities()} and {@link #toCompletePage()} methods will return less granular representations
 * (a stream of complete entities and a completely downloaded page respectively). These representations require
 * keeping more data in memory at once (full entities and full pages respectively) but they're often easier to work
 * with, trading memory efficiency for ease of use.
 * </p>
 *
 * <p>
 * Because of the stateful nature of this interface, each page can only be consumed in one of these ways. For example,
 * after calling {@link #toCompleteEntities()}, subscribing to the stream of chunks is impossible and will likely
 * throw an error.
 * </p>
 *
 * @param <PageHeader>   the type of the page header; in practice this will be either
 *                       * {@link io.datareplication.model.snapshot.SnapshotPageHeader} or
 *                       * {@link io.datareplication.model.feed.FeedPageHeader}
 * @param <EntityHeader> the type of the entity headers; see {@link Entity}
 */
public interface StreamingPage<
    PageHeader extends ToHttpHeaders,
    EntityHeader extends ToHttpHeaders
    > extends Flow.Publisher<StreamingPage.Chunk<EntityHeader>> {

    /**
     * An element in a {@link StreamingPage}.
     *
     * @param <EntityHeader> the type of the entity headers; see {@link Entity}
     */
    @SuppressWarnings("unused")
    class Chunk<EntityHeader> {
        private Chunk() {
        }

        /**
         * <p>
         * Represents the start of an entity. This contains the entity's headers and signals that following
         * {@link BodyChunk} elements form the body of this entity.
         * </p>
         *
         * <p>
         * May be followed by:
         * <ul>
         *     <li>{@link BodyChunk}: blocks of this entity's body</li>
         *     <li>{@link BodyEnd}: if the entity's body is empty (length 0), no body chunks are emitted and the
         *     header is immediately followed by {@link BodyEnd}</li>
         * </ul>
         * </p>
         *
         * @param <EntityHeader> the type of the entity headers; see {@link Entity}
         */
        @Value
        @AllArgsConstructor(access = AccessLevel.PRIVATE)
        @EqualsAndHashCode(callSuper = false)
        public static class Header<EntityHeader> extends Chunk<EntityHeader> {
            /**
             * The entity's headers.
             */
            @NonNull EntityHeader header;
            // TODO: Should content-type be optional? That would require making it optional in Body.
            /**
             * The entity's content type.
             */
            @NonNull ContentType contentType;
        }

        /**
         * <p>
         * A block of data of the current entity's body.
         * </p>
         *
         * <p>
         * May be followed by:
         * <ul>
         *     <li>{@link BodyChunk}: further body bytes</li>
         *     <li>{@link BodyEnd}: when the end of the entity's body is reached</li>
         * </ul>
         * </p>
         *
         * @param <EntityHeader> the type of the entity headers; see {@link Entity}
         */
        @Value
        @AllArgsConstructor(access = AccessLevel.PRIVATE)
        @EqualsAndHashCode(callSuper = false)
        public static class BodyChunk<EntityHeader> extends Chunk<EntityHeader> {
            /**
             * The chunk's byte data. The ByteBuffer will have its entire capacity used
             * (i.e. {@link ByteBuffer#position()} == 0 and {@link ByteBuffer#limit() == capacity}). When returned
             * from {@link StreamingPage}, the buffer will not be shared and may be freely modified.
             */
            @NonNull ByteBuffer data;
        }

        /**
         * <p>
         * Represents the end of an entity. It signals that the current entity's entire body has been read and
         * returned.
         * </p>
         *
         * <p>
         * May be followed by:
         * <ul>
         *     <li>{@link Header}: begin a new entity</li>
         *     <li>nothing: after the {@link BodyEnd} for the final entity on the page, the stream will terminate</li>
         * </ul>
         * </p>
         *
         * @param <EntityHeader>
         */
        @Value
        @AllArgsConstructor(access = AccessLevel.PRIVATE)
        @EqualsAndHashCode(callSuper = false)
        public static class BodyEnd<EntityHeader> extends Chunk<EntityHeader> {
            private static final BodyEnd<?> INSTANCE = new BodyEnd<>();
        }

        /**
         * Create a new {@link Header}.
         *
         * @param header         the entity's header object
         * @param contentType    the entity's content type
         * @param <EntityHeader> the entity's header
         * @return a new {@link Header}
         */
        public static @NonNull <EntityHeader> Header<EntityHeader> header(
            @NonNull EntityHeader header,
            @NonNull ContentType contentType) {
            return new Header<>(header, contentType);
        }

        /**
         * Create a new {@link BodyChunk}.
         *
         * @param data           the data chunk; this buffer <strong>should</strong> be {@link ByteBuffer#slice()}d and not
         *                       shared to avoid ByteBuffer's mutability leaking out and causing problems
         * @param <EntityHeader> the entity's header
         * @return a new {@link BodyChunk}
         */
        public static @NonNull <EntityHeader> BodyChunk<EntityHeader> bodyChunk(@NonNull ByteBuffer data) {
            return new BodyChunk<>(data);
        }

        /**
         * Return a {@link BodyEnd} marker.
         *
         * @param <EntityHeader> the entity's header
         * @return a {@link BodyEnd} marker
         */
        public static @NonNull <EntityHeader> BodyEnd<EntityHeader> bodyEnd() {
            //noinspection unchecked
            return (BodyEnd<EntityHeader>) BodyEnd.INSTANCE;
        }
    }

    /**
     * The page's headers.
     */
    @NonNull PageHeader header();

    /**
     * The boundary string for the page's multipart representation.
     */
    @NonNull String boundary();

    /**
     * <p>Wrap this page in a stream of complete {@link Entity} objects. The returned {@link Flow.Publisher} will
     * collect the full body for an entity and return it all at once. Naturally, this requires buffering the entire
     * body of each entity in memory, which might cause memory pressure for large entities.</p>
     *
     * <p>This method consumes the stream. After calling this method, subscribing to the page itself or calling either
     * {@link #toCompletePage()} or this method again is not allowed and will return an error.</p>
     *
     * @return a {@link Flow.Publisher} providing a stream of complete {@link Entity} objects
     */
    default @NonNull Flow.Publisher<@NonNull Entity<EntityHeader>> toCompleteEntities() {
        final ToCompleteEntitiesTransformer<EntityHeader> transformer = new ToCompleteEntitiesTransformer<>();
        final Flowable<Entity<EntityHeader>> flowable = Flowable
            .fromPublisher(FlowAdapters.toPublisher(this))
            .map(transformer::transform)
            .flatMapMaybe(Maybe::fromOptional);
        return FlowAdapters.toFlowPublisher(flowable);
    }

    /**
     * <p>Fully download this page and return a {@link Page} of its entities. The returned object contains the entire
     * contents of the page in memory. Naturally, keeping all bodies of all entities on the page in memory might
     * cause memory pressure for large pages.</p>
     *
     * <p>This method consumes the stream. After calling this method, subscribing to the page itself or calling either
     * {@link #toCompleteEntities()} or this method again is not allowed and will return an error.</p>
     *
     * @return a {@link Page} containing the complete contents of this page
     */
    default @NonNull CompletionStage<@NonNull Page<PageHeader, EntityHeader>> toCompletePage() {
        return Flowable
            .fromPublisher(FlowAdapters.toPublisher(toCompleteEntities()))
            .toList()
            .map(entities -> new Page<>(header(), boundary(), entities))
            .toCompletionStage();
    }
}
