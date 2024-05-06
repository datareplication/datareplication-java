package io.datareplication.consumer.feed;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.Value;

import java.time.Instant;

public class StartFrom {
    private StartFrom() {
    }

    @Value
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false)
    public static class Timestamp extends StartFrom {
        @NonNull Instant timestamp;
    }

    @Value
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false)
    public static class ContentId extends StartFrom {
        @NonNull io.datareplication.model.feed.ContentId contentId;
        @NonNull Instant timestamp;
    }

    @Value
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false)
    public static class Beginning extends StartFrom {
        private static final Beginning INSTANCE = new Beginning();
    }

    public static @NonNull Timestamp timestamp(@NonNull Instant timestamp) {
        return new Timestamp(timestamp);
    }

    public static @NonNull ContentId contentId(@NonNull io.datareplication.model.feed.ContentId contentId,
                                               @NonNull Instant timestamp) {
        return new ContentId(contentId, timestamp);
    }

    public static @NonNull Beginning beginning() {
        return Beginning.INSTANCE;
    }
}
