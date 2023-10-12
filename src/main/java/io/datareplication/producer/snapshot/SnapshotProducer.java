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
        private int maxWeightPerPage = 100;

        public @NonNull SnapshotProducer build(final SnapshotIndexRepository snapshotIndexRepository,
                                               final SnapshotPageRepository snapshotPageRepository,
                                               final SnapshotPageUrlBuilder snapshotPageUrlBuilder) {
            return build(snapshotIndexRepository, snapshotPageRepository, snapshotPageUrlBuilder, Clock.systemUTC());
        }

        public @NonNull SnapshotProducer build(final SnapshotIndexRepository snapshotIndexRepository,
                                               final SnapshotPageRepository snapshotPageRepository,
                                               final SnapshotPageUrlBuilder snapshotPageUrlBuilder,
                                               final Clock clock) {
            return new SnapshotProducerImpl(
                snapshotPageUrlBuilder,
                snapshotIndexRepository,
                snapshotPageRepository,
                pageIdProvider,
                snapshotIdProvider,
                maxWeightPerPage,
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

        public Builder maxWeightPerPage(final int maxWeightPerPage) {
            this.maxWeightPerPage = maxWeightPerPage;
            return this;
        }
    }

    static @NonNull SnapshotProducer.Builder builder() {
        return new SnapshotProducer.Builder();
    }
}
