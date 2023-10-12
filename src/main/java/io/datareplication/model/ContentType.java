package io.datareplication.model;

import lombok.NonNull;
import lombok.Value;

/**
 * A MIME type as used for an HTTP <code>Content-Type</code> header.
 */
@Value(staticConstructor = "of")
public class ContentType {
    @NonNull String value;
}
