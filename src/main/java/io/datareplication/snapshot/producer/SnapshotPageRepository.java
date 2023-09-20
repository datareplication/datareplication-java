package io.datareplication.snapshot.producer;

import io.datareplication.model.Page;
import io.datareplication.model.snapshot.SnapshotEntityHeader;
import io.datareplication.model.snapshot.SnapshotPageHeader;
import lombok.NonNull;

import java.util.concurrent.CompletionStage;

public interface SnapshotPageRepository {
    @NonNull CompletionStage<Void> save(@NonNull Page<@NonNull SnapshotPageHeader, @NonNull SnapshotEntityHeader> page);
}
