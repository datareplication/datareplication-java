package io.datareplication.internal.multipart;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Value;

import java.nio.ByteBuffer;

/**
 * An item returned by {@link MultipartParser} representing different parts of a multipart document.
 */
public class Token {
    private Token() {
    }

    /**
     * <p><code>Continue</code> signals that the parser has consumed some input that didn't contain any token. It's mostly
     * used to skip over prologue and epilogue (unstructured bytes before the opening delimiter and after the closing
     * delimiter of the document). Consumers generally want to ignore it and keep going.</p>
     *
     * <p>Can follow anything and be followed by anything.</p>
     */
    @Value
    @EqualsAndHashCode(callSuper = false)
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Continue extends Token {
        public static final Continue INSTANCE = new Continue();
    }

    /**
     * Start of a new part in the document. Can be followed by {@link Header} or {@link DataBegin}.
     */
    @Value
    @EqualsAndHashCode(callSuper = false)
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class PartBegin extends Token {
        public static final PartBegin INSTANCE = new PartBegin();
    }

    /**
     * A single header line. Can be followed by another Header or {@link DataBegin}.
     */
    @Value
    @EqualsAndHashCode(callSuper = false)
    public static class Header extends Token {
        @NonNull String name;
        @NonNull String value;
    }

    /**
     * Signals the end of the headers and start of the of body of the part. Can be followed by {@link Data} or
     * {@link PartEnd} (if the body is empty, i.e. length 0).
     */
    @Value
    @EqualsAndHashCode(callSuper = false)
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class DataBegin extends Token {
        public static final DataBegin INSTANCE = new DataBegin();
    }

    /**
     * A data block in a body. Can be followed by another Data or {@link PartEnd}.
     */
    @Value
    @EqualsAndHashCode(callSuper = false)
    public static class Data extends Token {
        @NonNull ByteBuffer data;
    }

    /**
     * Signals the end of a part. Can be followed by a new {@link PartBegin} or nothing (except for {@link Continue}) if
     * this was the final part in the document.
     */
    @Value
    @EqualsAndHashCode(callSuper = false)
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class PartEnd extends Token {
        public static final PartEnd INSTANCE = new PartEnd();
    }
}
