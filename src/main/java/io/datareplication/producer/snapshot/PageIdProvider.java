package io.datareplication.producer.snapshot;

import io.datareplication.model.PageId;

interface PageIdProvider {
    PageId newPageId();
}
