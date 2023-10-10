package io.datareplication.producer.snapshot;

import io.datareplication.model.Entity;
import io.datareplication.model.snapshot.SnapshotEntityHeader;
import io.datareplication.model.snapshot.SnapshotIndex;
import lombok.NonNull;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;

public interface SnapshotProducer {
    @NonNull CompletionStage<@NonNull SnapshotIndex> produce(
        @NonNull Flow.Publisher<@NonNull Entity<@NonNull SnapshotEntityHeader>> entities);

    class Builder {

        // TODO: Default impl
        private PageIdProvider pageIdProvider = new UUIDPageIdProvider();
        // TODO: Default impl
        private SnapshotIdProvider snapshotIdProvider = new UUIDSnapshotIdProvider();
        private int maxWeightPerPage = 100;

        public @NonNull SnapshotProducer build(final SnapshotIndexRepository snapshotIndexRepository,
                                               final SnapshotPageRepository snapshotPageRepository) {
            return new SnapshotProducerImpl(
                snapshotIndexRepository,
                snapshotPageRepository,
                pageIdProvider,
                snapshotIdProvider,
                maxWeightPerPage
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
