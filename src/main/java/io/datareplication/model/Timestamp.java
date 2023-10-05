package io.datareplication.model;

import lombok.NonNull;
import lombok.Value;

import java.time.Instant;

@Value(staticConstructor = "of")
public class Timestamp {
    @NonNull Instant value;
}
