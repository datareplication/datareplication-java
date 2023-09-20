package io.datareplication.model;

import lombok.Value;

import java.time.Instant;

@Value(staticConstructor = "of")
public class Timestamp {
    Instant value;
}
