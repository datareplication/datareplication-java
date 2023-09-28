package io.datareplication.model.snapshot;

import io.datareplication.model.Body;
import io.datareplication.model.Timestamp;
import io.datareplication.model.Url;
import lombok.NonNull;
import lombok.Value;

import java.util.Collections;
import java.util.List;

@Value
public class SnapshotIndex {
    @NonNull SnapshotId id;
    @NonNull Timestamp started;
    @NonNull List<@NonNull Url> pages;

    public SnapshotIndex(@NonNull SnapshotId id, @NonNull Timestamp started, @NonNull List<@NonNull Url> pages) {
        this.id = id;
        this.started = started;
        this.pages = Collections.unmodifiableList(pages);
    }

    public @NonNull Body toJson() {
        throw new RuntimeException("not implemented");
    }

    // TODO: error handling
    public static @NonNull SnapshotIndex fromJson(@NonNull Body json) {
        throw new RuntimeException("not implemented");
    }
}
