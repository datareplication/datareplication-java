package io.datareplication.producer.snapshot;

import io.datareplication.model.snapshot.SnapshotId;

interface SnapshotIdProvider {
    SnapshotId newSnapshotId();
}
