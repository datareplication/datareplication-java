package io.datareplication.producer.snapshot;

import io.datareplication.model.snapshot.SnapshotId;

import java.util.UUID;

/**
 * The default implementation for a {@link SnapshotIdProvider}.
 * Uses <code>UUID.randomUUID()</code> to create a unique pageId.
 * Will be used when nothing else is specified in the {@link SnapshotProducer}
 */
class UUIDSnapshotIdProvider implements SnapshotIdProvider {
    /**
     * @return a <code>UUID.randomUUID()</code> the create a unique pageId
     */
    @Override
    public SnapshotId newSnapshotId() {
        return SnapshotId.of(UUID.randomUUID().toString());
    }
}
