package io.datareplication.model.snapshot;

import io.datareplication.model.Body;
import io.datareplication.model.Url;
import lombok.Value;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

@Value
public class SnapshotIndex {
    SnapshotId id;
    Instant lastModified;
    List<Url> pages;

    public SnapshotIndex(SnapshotId id, Instant lastModified, List<Url> pages) {
        this.id = id;
        this.lastModified = lastModified;
        this.pages = Collections.unmodifiableList(pages);
    }

    public Body toJson() {
        throw new RuntimeException("not implemented");
    }

    // TODO: error handling
    public static SnapshotIndex fromJson(Body json) {
        throw new RuntimeException("not implemented");
    }
}
