package io.datareplication.internal.multipart;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Value;

import java.nio.ByteBuffer;

public abstract class Elem {
    private Elem() {
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Continue extends Elem {
        public static final Continue INSTANCE = new Continue();
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class PartBegin extends Elem {
        public static final PartBegin INSTANCE = new PartBegin();
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    public static class Header extends Elem {
        @NonNull String name;
        @NonNull String value;
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class DataBegin extends Elem {
        public static final DataBegin INSTANCE = new DataBegin();
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    public static class Data extends Elem {
        @NonNull ByteBuffer data;
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class PartEnd extends Elem {
        public static final PartEnd INSTANCE = new PartEnd();
    }
}
