package io.datareplication.model.feed;

import io.datareplication.model.Url;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.Value;

public abstract class Link {
    private Link() {
    }

    abstract @NonNull Url value();

    @Value
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false)
    public static class Prev extends Link {
        @NonNull Url value;
    }

    @Value
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false)
    public static class Next extends Link {
        @NonNull Url value;
    }

    @Value
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false)
    public static class Self extends Link {
        @NonNull Url value;
    }

    public static @NonNull Prev prev(Url value) {
        return new Prev(value);
    }

    public static @NonNull Next next(Url value) {
        return new Next(value);
    }

    public static @NonNull Self self(Url value) {
        return new Self(value);
    }
}
