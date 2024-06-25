package io.datareplication.model;

import lombok.NonNull;
import lombok.Value;

/**
 * The ID of a feed or snapshot page.
 * <p>
 * Note that this is only used on the producer side; on the consumer side, the concept of a page ID doesn't exist
 * any more and pages are identified only by their URL.
 */
@Value(staticConstructor = "of")
public class PageId {
    @NonNull String value;
}
