package io.datareplication.model.feed;

import io.datareplication.model.Url;
import lombok.Value;

@Value(staticConstructor = "of")
public class Link {
    Url value;
}
