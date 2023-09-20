package io.datareplication.model;

import lombok.NonNull;
import lombok.Value;

@Value(staticConstructor = "of")
public class Url {
    @NonNull
    String value;
}
