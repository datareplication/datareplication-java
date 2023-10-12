package io.datareplication.producer.snapshot;

import io.datareplication.model.snapshot.SnapshotId;

public interface SnapshotIdProvider {
    SnapshotId newSnapshotId();
}
