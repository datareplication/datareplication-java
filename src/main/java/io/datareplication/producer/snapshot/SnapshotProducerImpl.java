package io.datareplication.producer.snapshot;

import io.datareplication.model.Entity;
import io.datareplication.model.snapshot.SnapshotEntityHeader;
import io.datareplication.model.snapshot.SnapshotIndex;
import lombok.NonNull;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;

// TODO: impl the impl
class SnapshotProducerImpl implements SnapshotProducer {
    SnapshotProducerImpl(final SnapshotIndexRepository snapshotIndexRepository,
                         final SnapshotPageRepository snapshotPageRepository,
                         final PageIdProvider pageIdProvider,
                         final SnapshotIdProvider snapshotIdProvider,
                         final int maxWeightPerPage) {

    }

    @Override
    public @NonNull CompletionStage<@NonNull SnapshotIndex> produce(
        final @NonNull Flow.Publisher<@NonNull Entity<@NonNull SnapshotEntityHeader>> entities
    ) {
        throw new RuntimeException("not implemented");
    }
}
