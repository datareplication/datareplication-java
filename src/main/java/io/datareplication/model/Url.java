package io.datareplication.model;

import lombok.NonNull;
import lombok.Value;

/**
 * A URL.
 */
@Value(staticConstructor = "of")
public class Url {
    @NonNull String value;
}
