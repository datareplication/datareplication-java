package io.datareplication.model;

import lombok.Value;

@Value(staticConstructor = "of")
public class Header {
    String name;
    String value;

    public static final String CONTENT_TYPE = "content-type";
    public static final String CONTENT_LENGTH = "content-length";
    public static final String LAST_MODIFIED = "last-modified";
}
