package io.datareplication.consumer.feed;

import io.datareplication.model.Entity;
import io.datareplication.model.Url;
import io.datareplication.model.feed.FeedEntityHeader;
import io.datareplication.model.feed.FeedPageHeader;
import io.datareplication.consumer.StreamingPage;
import lombok.NonNull;

import java.util.concurrent.Flow;

public interface FeedConsumer {
    // TODO: error handling
    @NonNull Flow.Publisher<@NonNull StreamingPage<@NonNull FeedPageHeader, @NonNull FeedEntityHeader>> streamPages(@NonNull Url url,
                                                                                                                    @NonNull StartFrom startFrom);

    @NonNull Flow.Publisher<@NonNull Entity<@NonNull FeedEntityHeader>> streamEntities(@NonNull Url url,
                                                                                       @NonNull StartFrom startFrom);
}
