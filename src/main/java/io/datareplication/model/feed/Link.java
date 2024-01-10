package io.datareplication.model.feed;

import io.datareplication.model.Url;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.Value;

/**
 * Represents the link from a feed page.
 * Each page must have a "link; rel=self" set.
 * If they exist, the links to the previous "link; rel=prev" and next "link; rel=next" pages must also be present.
 */
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

    public enum Rel {
        /**
         * Link to this page must always be set
         */
        SELF,
        /**
         * Link to the next page. If this is not set, it means that the consumer has arrived at the last page
         */
        NEXT,
        /**
         * Link to the previous page. If this is not set, this means that this is the first (oldest) page of the feed
         */
        PREV;

        @Override
        public String toString() {
            switch (this) {
                case SELF:
                    return "self";
                case NEXT:
                    return "next";
                case PREV:
                    return "prev";
                default:
                    throw new UnsupportedOperationException("unknown value");
            }
        }
    }
}
