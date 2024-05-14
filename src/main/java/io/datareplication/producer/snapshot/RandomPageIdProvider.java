package io.datareplication.producer.snapshot;

import io.datareplication.model.PageId;

import java.util.UUID;

/**
 * The default implementation for a {@link RandomPageIdProvider}.
 * Uses <code>UUID.randomUUID()</code> to create a unique pageId.
 * Will be used when nothing else is specified in the {@link SnapshotProducer}
 */
class RandomPageIdProvider {
    /**
     * @return a <code>UUID.randomUUID()</code> the create a unique pageId
     */
    public PageId newPageId() {
        return PageId.of(UUID.randomUUID().toString());
    }
}
