package io.datareplication.producer.snapshot;

import io.datareplication.model.PageId;

public interface PageIdProvider {
    PageId newPageId();
}
