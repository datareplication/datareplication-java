package io.datareplication.producer.snapshot;

import io.datareplication.model.snapshot.SnapshotIndex;
import lombok.NonNull;

import java.util.concurrent.CompletionStage;

public interface SnapshotIndexRepository {
    @NonNull CompletionStage<Void> save(@NonNull SnapshotIndex snapshotIndex);
}
