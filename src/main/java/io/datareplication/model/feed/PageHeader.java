package io.datareplication.model.feed;

import io.datareplication.model.HttpHeaders;
import lombok.Value;

import java.time.Instant;
import java.util.Optional;

@Value
public class PageHeader {
    Instant lastModified;
    Link self;
    Optional<Link> prev;
    Optional<Link> next;
    HttpHeaders extraHeaders;
}
