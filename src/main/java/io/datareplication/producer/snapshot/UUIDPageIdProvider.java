package io.datareplication.producer.snapshot;

import io.datareplication.model.PageId;

import java.util.UUID;

/**
 * The default implementation for a {@link PageIdProvider}.
 * Uses <code>UUID.randomUUID()</code> to create a unique pageId.
 * Will be used when nothing else is specified in the {@link SnapshotProducer}
 */
public class UUIDPageIdProvider implements PageIdProvider {
    /**
     * @return a <code>UUID.randomUUID()</code> the create a unique pageId
     */
    @Override
    public PageId newPageId() {
        return PageId.of(UUID.randomUUID().toString());
    }
}
