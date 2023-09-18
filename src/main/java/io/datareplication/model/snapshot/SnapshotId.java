package io.datareplication.model.snapshot;

import lombok.Value;

@Value(staticConstructor = "of")
public class SnapshotId {
    String value;
}
