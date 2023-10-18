package io.datareplication.producer.snapshot;

import io.datareplication.model.Entity;
import io.datareplication.model.Timestamp;
import io.datareplication.model.snapshot.SnapshotEntityHeader;
import io.datareplication.model.snapshot.SnapshotId;
import io.datareplication.model.snapshot.SnapshotIndex;
import lombok.AllArgsConstructor;
import lombok.NonNull;

import java.time.Clock;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;

// TODO: impl the impl
@AllArgsConstructor
class SnapshotProducerImpl implements SnapshotProducer {
    private final SnapshotPageUrlBuilder snapshotPageUrlBuilder;
    private final SnapshotIndexRepository snapshotIndexRepository;
    private final SnapshotPageRepository snapshotPageRepository;
    private final PageIdProvider pageIdProvider;
    private final SnapshotIdProvider snapshotIdProvider;
    private final long maxBytesPerPage;
    private final Clock clock;

    @Override
    public @NonNull CompletionStage<@NonNull SnapshotIndex> produce(
        final @NonNull Flow.Publisher<@NonNull Entity<@NonNull SnapshotEntityHeader>> entities
    ) {
        SnapshotId id = snapshotIdProvider.newSnapshotId();
        Timestamp createdAt = Timestamp.of(clock.instant());
        SnapshotIndex snapshotIndex = new SnapshotIndex(id, createdAt, Collections.emptyList());
        return CompletableFuture.completedFuture(snapshotIndex);
    }
}
