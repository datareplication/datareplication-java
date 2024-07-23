package io.datareplication.producer.feed;

import io.datareplication.model.PageId;

import java.util.UUID;

/**
 * Internal class that generates a new page ID (split out for testing).
 */
class RandomPageIdProvider {
    PageId newPageId() {
        return PageId.of(UUID.randomUUID().toString());
    }
}
