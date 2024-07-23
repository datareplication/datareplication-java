package io.datareplication.producer.feed;

import io.datareplication.model.feed.ContentId;

import java.util.UUID;

/**
 * Internal class that generates a new content ID (split out for testing).
 */
class RandomContentIdProvider {
    ContentId newContentId() {
        final var randomPart = UUID.randomUUID();
        return ContentId.of(String.format("%s-datareplication-java@datareplication.io", randomPart));
    }
}
