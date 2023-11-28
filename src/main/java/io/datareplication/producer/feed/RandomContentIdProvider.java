package io.datareplication.producer.feed;

import io.datareplication.model.feed.ContentId;

import java.util.UUID;

class RandomContentIdProvider {
    ContentId newContentId() {
        final var randomPart = UUID.randomUUID();
        return ContentId.of(String.format("%s-datareplication-java@datareplication.io", randomPart));
    }
}
