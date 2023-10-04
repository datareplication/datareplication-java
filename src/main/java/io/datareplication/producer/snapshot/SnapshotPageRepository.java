package io.datareplication.producer.snapshot;

import io.datareplication.model.Page;
import io.datareplication.model.PageId;
import io.datareplication.model.snapshot.SnapshotEntityHeader;
import io.datareplication.model.snapshot.SnapshotId;
import io.datareplication.model.snapshot.SnapshotPageHeader;
import lombok.NonNull;

import java.util.concurrent.CompletionStage;

public interface SnapshotPageRepository {
    @NonNull CompletionStage<Void> save(@NonNull SnapshotId snapshotId,
                                        @NonNull PageId pageId,
                                        @NonNull Page<@NonNull SnapshotPageHeader, @NonNull SnapshotEntityHeader> page);
}
