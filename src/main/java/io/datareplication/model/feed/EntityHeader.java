package io.datareplication.model.feed;

import io.datareplication.model.HttpHeaders;
import lombok.Value;

import java.time.Instant;
import java.util.Optional;

@Value
public class EntityHeader {
    Instant lastModified;
    OperationType operationType;
    Optional<ContentId> contentId;
    HttpHeaders extraHeaders;
}
