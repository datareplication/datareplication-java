package io.datareplication.model.snapshot;

import io.datareplication.model.HttpHeaders;
import lombok.Value;

import java.time.Instant;

@Value
public class EntityHeader {
    Instant lastModified;
    HttpHeaders extraHeaders;
}
