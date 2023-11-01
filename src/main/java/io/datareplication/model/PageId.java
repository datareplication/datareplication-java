package io.datareplication.model;

import lombok.NonNull;
import lombok.Value;

/**
 * <p>The ID of a feed or snapshot page.</p>
 *
 * <p>
 *     Note that this is only used on the producer side; on the consumer side, the concept of a page ID doesn't exist
 *     any more and pages are identified only by their URL.
 * </p>
 * <p>
 *     The boundary() method is a helper method for creating boundary Strings which are derived from the ID, but with
 *     traditional <i>_---_</i> prefixed to it for distinction in the output of the page.
 * </p>
 */
@Value(staticConstructor = "of")
public class PageId {
    @NonNull String value;

    public String boundary() {
        return String.format("_---_%s", value);
    }
}
