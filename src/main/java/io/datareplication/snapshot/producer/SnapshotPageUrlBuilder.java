package io.datareplication.snapshot.producer;

import io.datareplication.model.PageId;
import io.datareplication.model.Url;
import io.datareplication.model.snapshot.SnapshotId;
import lombok.NonNull;

public interface SnapshotPageUrlBuilder {
    // TODO: if we add IDs to the header, pass entire page header?
    @NonNull Url pageUrl(@NonNull SnapshotId snapshotId, @NonNull PageId pageId);
}
