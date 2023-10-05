package io.datareplication.model.feed;

import io.datareplication.model.Url;
import lombok.NonNull;
import lombok.Value;

/**
 * A link in the header of a feed page.
 */
@Value(staticConstructor = "of")
public class Link {
    @NonNull Url value;
}
