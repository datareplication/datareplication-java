package io.datareplication.producer.feed;

import io.datareplication.model.PageId;

import java.util.UUID;

class RandomPageIdProvider {
    PageId newPageId() {
        return PageId.of(UUID.randomUUID().toString());
    }
}
