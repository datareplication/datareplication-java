package io.datareplication.producer.snapshot;

import io.datareplication.model.Entity;
import io.datareplication.model.snapshot.SnapshotEntityHeader;
import io.datareplication.model.snapshot.SnapshotIndex;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;

/**
 * A <code>SnapshotProducer</code> produces a snapshot index, which consists of a list of pages which contain
 * all entities.
 * <p>
 * In order to produce a snapshot, you provide a stream of entities.
 * <p>
 * A snapshot producer is created using a builder: call the
 * {@link #builder(SnapshotIndexRepository, SnapshotPageRepository, SnapshotPageUrlBuilder)}
 * method to create a new builder with default settings, call the methods on {@link Builder} to customize the producer,
 * then call {@link Builder#build()} to create a new {@link SnapshotProducer} instance.
 */
public interface SnapshotProducer {
    /**
     * Produces a snapshot of given stream of entities.
     *
     * @param entities the entities as a stream which will be included in the snapshot.
     * @return a {@link SnapshotIndex}
     */
    @NonNull CompletionStage<@NonNull SnapshotIndex> produce(
        @NonNull Flow.Publisher<@NonNull Entity<@NonNull SnapshotEntityHeader>> entities);

    /**
     * A builder for {@link SnapshotProducer}.
     *
     * <p>Each of the builder methods modifies the state of the builder and returns the same instance. Because of that,
     * builders are not thread-safe.</p>
     *
     * @see SnapshotProducer#builder(SnapshotIndexRepository, SnapshotPageRepository, SnapshotPageUrlBuilder)
     */
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    class Builder {
        private final SnapshotIndexRepository snapshotIndexRepository;
        private final SnapshotPageRepository snapshotPageRepository;
        private final SnapshotPageUrlBuilder snapshotPageUrlBuilder;
        private Clock clock = Clock.systemUTC();
        private long maxBytesPerPage = 1000L * 1000L;
        private long maxEntitiesPerPage = Long.MAX_VALUE;

        /**
         * Use the given {@link Clock} when generating timestamps for new entities.
         *
         * @param clock the clock to use for timestamps
         * @return this builder
         */
        public @NonNull SnapshotProducer.Builder clock(@NonNull Clock clock) {
            this.clock = clock;
            return this;
        }

        /**
         * Set the maximum bytes per page. When a page is composed, a new page will be created if the current page
         * gets too big. Defaults to 1 MB.
         *
         * @param maxBytesPerPage the maximum bytes per page. Must be equal or greater than 1.
         * @return this builder
         * @throws IllegalArgumentException if the argument is &lt; 1
         */
        public @NonNull Builder maxBytesPerPage(final long maxBytesPerPage) {
            if (maxBytesPerPage <= 0) {
                throw new IllegalArgumentException("maxBytesPerPage must be >= 1");
            }
            this.maxBytesPerPage = maxBytesPerPage;
            return this;
        }

        /**
         * Set the maximum entities per page. When a page is composed, a new page will be created if the current page
         * has too many entities. Defaults to Long.MAX_VALUE (meaning no limit).
         *
         * @param maxEntitiesPerPage the maximum number of entities per page. Must be equal or greater than 1.
         * @return this builder
         * @throws IllegalArgumentException if the argument is &lt; 1
         */
        public @NonNull Builder maxEntitiesPerPage(final long maxEntitiesPerPage) {
            if (maxEntitiesPerPage <= 0) {
                throw new IllegalArgumentException("maxEntitiesPerPage must be >= 1");
            }
            this.maxEntitiesPerPage = maxEntitiesPerPage;
            return this;
        }

        /**
         * Build a new {@link SnapshotProducer} with the parameters set on this builder.
         *
         * @return a new {@link SnapshotProducer}
         */
        public @NonNull SnapshotProducer build() {
            return new SnapshotProducerImpl(
                snapshotPageUrlBuilder,
                snapshotIndexRepository,
                snapshotPageRepository,
                new UUIDPageIdProvider(),
                new UUIDSnapshotIdProvider(),
                maxBytesPerPage,
                maxEntitiesPerPage,
                clock
            );
        }
    }

    /**
     * Create a new {@link SnapshotProducer.Builder} with default settings. Use the
     * {@link Builder#build()} method on
     * the returned builder to create a {@link SnapshotProducer} with the specified settings.
     *
     * @param snapshotIndexRepository the repository where the snapshot indexes are stored.
     * @param snapshotPageRepository the repository where the pages of the corresponding snapshot are stored.
     * @param snapshotPageUrlBuilder the builder, which build the urls of the pages of a snapshot.
     *
     * @return a new builder
     */
    static @NonNull SnapshotProducer.Builder builder(@NonNull final SnapshotIndexRepository snapshotIndexRepository,
                                                     @NonNull final SnapshotPageRepository snapshotPageRepository,
                                                     @NonNull final SnapshotPageUrlBuilder snapshotPageUrlBuilder) {
        return new SnapshotProducer.Builder(snapshotIndexRepository, snapshotPageRepository, snapshotPageUrlBuilder);
    }
}
