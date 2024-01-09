package io.datareplication.producer.snapshot;

import io.datareplication.model.Entity;
import io.datareplication.model.snapshot.SnapshotEntityHeader;
import io.datareplication.model.snapshot.SnapshotIndex;
import lombok.NonNull;

import java.time.Clock;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;

/**
 * <p>A <code>SnapshotProducer</code> produces a snapshot index, which consists of a list of pages which contain
 * all entities.</p>
 *
 * <p>In order to produce a snapshot, you provide a stream of entities.
 *
 * <p>A snapshot producer is created using a builder: call the {@link #builder()} method to create a new builder
 * with default settings, call the methods on {@link Builder} to customize the producer, then call
 * {@link Builder#build(SnapshotIndexRepository, SnapshotPageRepository, SnapshotPageUrlBuilder)} to create a new
 * {@link SnapshotProducer} instance.</p>
 */
public interface SnapshotProducer {

    /**
     * Produces a snapshot of given stream of entities.
     *
     * @param entities the entities as a stream which which will be included in the snapshot.
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
     * @see SnapshotProducer#builder()
     */
    class Builder {
        private PageIdProvider pageIdProvider = new UUIDPageIdProvider();
        private SnapshotIdProvider snapshotIdProvider = new UUIDSnapshotIdProvider();
        private long maxBytesPerPage = 1000L * 1000L;
        private long maxEntriesPerPage = Long.MAX_VALUE;

        /**
         * Build a new {@link SnapshotProducer} with the parameters set on this builder.
         *
         * @param snapshotIndexRepository the repository where the snapshot indexes are stored.
         * @param snapshotPageRepository the repository where the pages of the corresponding snapshot are stored.
         * @param snapshotPageUrlBuilder the builder, which build the urls of the pages of a snapshot.
         *
         * @return a new {@link SnapshotProducer}
         */
        public @NonNull SnapshotProducer build(@NonNull final SnapshotIndexRepository snapshotIndexRepository,
                                               @NonNull final SnapshotPageRepository snapshotPageRepository,
                                               @NonNull final SnapshotPageUrlBuilder snapshotPageUrlBuilder) {
            return build(snapshotIndexRepository, snapshotPageRepository, snapshotPageUrlBuilder, Clock.systemUTC());
        }

        /**
         * Build a new {@link SnapshotProducer} with the parameters set on this builder.
         *
         * @param snapshotIndexRepository the repository where the snapshot indexes are stored.
         * @param snapshotPageRepository the repository where the pages of the corresponding snapshot are stored.
         * @param snapshotPageUrlBuilder the builder, which build the urls of the pages of a snapshot.
         * @param clock a clock can be set for testing purposes where time data is important.
         *
         * @return a new {@link SnapshotProducer}
         */
        public @NonNull SnapshotProducer build(@NonNull final SnapshotIndexRepository snapshotIndexRepository,
                                               @NonNull final SnapshotPageRepository snapshotPageRepository,
                                               @NonNull final SnapshotPageUrlBuilder snapshotPageUrlBuilder,
                                               @NonNull final Clock clock) {
            return new SnapshotProducerImpl(
                snapshotPageUrlBuilder,
                snapshotIndexRepository,
                snapshotPageRepository,
                pageIdProvider,
                snapshotIdProvider,
                maxBytesPerPage,
                maxEntriesPerPage,
                clock
            );
        }

        /**
         * Use the given {@link PageIdProvider} for PageID generation.
         *
         * @param pageIdProvider the pageIdProvider which generates IDs for Pages.
         * @return this builder
         */
        public @NonNull Builder pageIdProvider(@NonNull final PageIdProvider pageIdProvider) {
            this.pageIdProvider = pageIdProvider;
            return this;
        }

        /**
         * Use the given {@link PageIdProvider} for SnapshotID generation.
         *
         * @param snapshotIdProvider the snapshotIdProvider which generates IDs for Snapshots.
         * @return this builder
         */
        public @NonNull Builder snapshotIdProvider(@NonNull final SnapshotIdProvider snapshotIdProvider) {
            this.snapshotIdProvider = snapshotIdProvider;
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
         * Set the maximum entries per page. When a page is composed, a new page will be created if the current page
         * has too many entries. Defaults to Long.MAX_VALUE (meaning no limit).
         *
         * @param maxEntriesPerPage the maximum entries per page. Must be equal or greater than 1.
         * @return this builder
         * @throws IllegalArgumentException if the argument is &lt; 1
         */
        public @NonNull Builder maxEntriesPerPage(final long maxEntriesPerPage) {
            if (maxEntriesPerPage <= 0) {
                throw new IllegalArgumentException("maxEntriesPerPage must be >= 1");
            }
            this.maxEntriesPerPage = maxEntriesPerPage;
            return this;
        }
    }

    /**
     * Create a new {@link SnapshotProducer.Builder} with default settings. Use the
     * {@link Builder#build(SnapshotIndexRepository, SnapshotPageRepository, SnapshotPageUrlBuilder)} method on
     * the returned builder to create a {@link SnapshotProducer} with the specified settings.
     *
     * @return a new builder
     */
    static @NonNull SnapshotProducer.Builder builder() {
        return new SnapshotProducer.Builder();
    }
}
