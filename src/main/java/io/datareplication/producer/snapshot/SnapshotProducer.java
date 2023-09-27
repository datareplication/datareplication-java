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
}
