package io.datareplication.model.snapshot;

import io.datareplication.model.HttpHeaders;
import io.datareplication.model.ToHttpHeaders;
import lombok.NonNull;
import lombok.Value;

@Value
public class SnapshotPageHeader implements ToHttpHeaders {
    // TODO: Snapshot ID is probably ok, but I don't think we can have page IDs in here because the concept effectively
    //       doesn't exist any more on the consumer side. We could have a self link similar to feed pages for snapshot
    //       pages, but then should that be a required part of the HTTP format or just something we synthesise and never
    //       store in HTTP headers? And if so, would need to change FeedPageHeader so the fields are consistent.
    @NonNull SnapshotId snapshotId;
    @NonNull HttpHeaders extraHeaders;

    @Override
    public @NonNull HttpHeaders toHttpHeaders() {
        return extraHeaders;
    }
}
