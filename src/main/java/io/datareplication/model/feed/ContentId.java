package io.datareplication.model.feed;

import lombok.NonNull;
import lombok.Value;

@Value(staticConstructor = "of")
public class ContentId {
    @NonNull String value;
}
