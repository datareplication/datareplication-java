package io.datareplication.model;

import lombok.NonNull;
import lombok.Value;

@Value(staticConstructor = "of")
public class PageId {
    @NonNull String value;
}
