package io.datareplication.model.snapshot;

import io.datareplication.model.Body;
import io.datareplication.model.Timestamp;
import io.datareplication.model.Url;
import lombok.Value;

import java.util.Collections;
import java.util.List;

@Value
public class SnapshotIndex {
    SnapshotId id;
    Timestamp started;
    List<Url> pages;

    public SnapshotIndex(SnapshotId id, Timestamp started, List<Url> pages) {
        this.id = id;
        this.started = started;
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
