package io.datareplication.model.feed;

import lombok.Value;

@Value(staticConstructor = "of")
public class ContentId {
    String value;
}
