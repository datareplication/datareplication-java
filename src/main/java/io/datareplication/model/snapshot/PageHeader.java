package io.datareplication.model.snapshot;

import io.datareplication.model.HttpHeaders;
import lombok.Value;

@Value
public class PageHeader {
    HttpHeaders extraHeaders;
}
