package io.datareplication.consumer.feed;

import io.datareplication.consumer.StreamingPage;
import io.datareplication.internal.http.HttpClient;
import io.datareplication.model.Entity;
import io.datareplication.model.Url;
import io.datareplication.model.feed.FeedEntityHeader;
import io.datareplication.model.feed.FeedPageHeader;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;

import java.util.concurrent.Flow;

// TODO: Implement
@AllArgsConstructor(access = AccessLevel.PACKAGE)
public class FeedConsumerImpl implements FeedConsumer {
    private final HttpClient httpClient;
    @Override
    public @NonNull Flow.Publisher<
        @NonNull StreamingPage<@NonNull FeedPageHeader, @NonNull FeedEntityHeader>
        > streamPages(@NonNull final Url url,
                      @NonNull final StartFrom startFrom) {
        throw new UnsupportedOperationException("NIY");
    }

    @Override
    public @NonNull Flow.Publisher<
        @NonNull Entity<@NonNull FeedEntityHeader>
        > streamEntities(@NonNull final Url url,
                         @NonNull final StartFrom startFrom) {
        throw new UnsupportedOperationException("NIY");
    }
}
