package io.datareplication.producer.snapshot;

import io.datareplication.model.PageId;
import io.datareplication.model.Url;
import io.datareplication.model.snapshot.SnapshotId;
import lombok.NonNull;

public interface SnapshotPageUrlBuilder {
    @NonNull Url pageUrl(@NonNull SnapshotId snapshotId, @NonNull PageId pageId);
}
