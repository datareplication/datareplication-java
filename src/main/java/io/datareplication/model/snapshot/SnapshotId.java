package io.datareplication.model.snapshot;

import lombok.NonNull;
import lombok.Value;

@Value(staticConstructor = "of")
public class SnapshotId {
    @NonNull
    String value;
}
