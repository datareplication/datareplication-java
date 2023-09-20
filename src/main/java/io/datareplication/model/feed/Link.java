package io.datareplication.model.feed;

import io.datareplication.model.Url;
import lombok.NonNull;
import lombok.Value;

@Value(staticConstructor = "of")
public class Link {
    @NonNull
    Url value;
}
