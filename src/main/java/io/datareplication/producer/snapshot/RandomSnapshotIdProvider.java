package io.datareplication.producer.snapshot;

import io.datareplication.model.snapshot.SnapshotId;

import java.util.UUID;

/**
 * The default implementation for a {@link RandomSnapshotIdProvider}.
 * Uses <code>UUID.randomUUID()</code> to create a unique pageId.
 * Will be used when nothing else is specified in the {@link SnapshotProducer}
 */
class RandomSnapshotIdProvider {
    /**
     * @return a <code>UUID.randomUUID()</code> the create a unique pageId
     */
    public SnapshotId newSnapshotId() {
        return SnapshotId.of(UUID.randomUUID().toString());
    }
}
