package io.datareplication.model.snapshot;

import lombok.NonNull;
import lombok.Value;

/**
 * The ID of a snapshot.
 */
@Value(staticConstructor = "of")
public class SnapshotId {
    @NonNull String value;
}
