package io.datareplication.producer.snapshot;

import io.datareplication.model.Entity;
import io.datareplication.model.snapshot.SnapshotEntityHeader;
import io.datareplication.model.snapshot.SnapshotIndex;
import lombok.NonNull;

import java.time.Clock;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;

public interface SnapshotProducer {
    @NonNull CompletionStage<@NonNull SnapshotIndex> produce(
        @NonNull Flow.Publisher<@NonNull Entity<@NonNull SnapshotEntityHeader>> entities);

    class Builder {
        private PageIdProvider pageIdProvider = new UUIDPageIdProvider();
        private SnapshotIdProvider snapshotIdProvider = new UUIDSnapshotIdProvider();
        private long maxBytesPerPage = 1000L * 1000L;
        public static final long MINIMUM_BYTES_PER_PAGE = 0L;
        private long maxEntriesPerPage = Long.MAX_VALUE;
        public static final long MINIMUM_ENTRIES_PER_PAGE = 0L;

        public @NonNull SnapshotProducer build(@NonNull final SnapshotIndexRepository snapshotIndexRepository,
                                               @NonNull final SnapshotPageRepository snapshotPageRepository,
                                               @NonNull final SnapshotPageUrlBuilder snapshotPageUrlBuilder) {
            return build(snapshotIndexRepository, snapshotPageRepository, snapshotPageUrlBuilder, Clock.systemUTC());
        }

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

        public Builder pageIdProvider(final PageIdProvider pageIdProvider) {
            this.pageIdProvider = pageIdProvider;
            return this;
        }

        public Builder snapshotIdProvider(final SnapshotIdProvider snapshotIdProvider) {
            this.snapshotIdProvider = snapshotIdProvider;
            return this;
        }

        public Builder maxBytesPerPage(final long maxBytesPerPage) {
            if (maxBytesPerPage > MINIMUM_BYTES_PER_PAGE) {
                this.maxBytesPerPage = maxBytesPerPage;
            }
            return this;
        }

        public Builder maxEntriesPerPage(final long maxEntriesPerPage) {
            if (maxEntriesPerPage > MINIMUM_ENTRIES_PER_PAGE) {
                this.maxEntriesPerPage = maxEntriesPerPage;
            }
            return this;
        }
    }

    static @NonNull SnapshotProducer.Builder builder() {
        return new SnapshotProducer.Builder();
    }
}
