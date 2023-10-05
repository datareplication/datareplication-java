package io.datareplication.model.feed;

import lombok.NonNull;
import lombok.Value;

/**
 * The content ID of a feed entity.
 */
@Value(staticConstructor = "of")
public class ContentId {
    @NonNull String value;
}
