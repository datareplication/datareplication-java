package io.datareplication.model;

import lombok.Value;

@Value(staticConstructor = "of")
public class Url {
    String value;
}
