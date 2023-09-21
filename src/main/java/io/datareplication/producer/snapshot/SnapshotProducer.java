package io.datareplication.producer.snapshot;

import io.datareplication.model.Entity;
import io.datareplication.model.snapshot.SnapshotEntityHeader;
import io.datareplication.model.snapshot.SnapshotIndex;
import lombok.NonNull;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;

public interface SnapshotProducer {
    // TODO: is this right?
    @NonNull Flow.Subscriber<@NonNull Entity<@NonNull SnapshotEntityHeader>> produce();

    // TODO: pick one, I think, having both is redundant and confusing
    @NonNull CompletionStage<@NonNull SnapshotIndex> produce(@NonNull Flow.Publisher<@NonNull Entity<@NonNull SnapshotEntityHeader>> entities);
}
