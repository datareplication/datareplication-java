package io.datareplication.consumer.feed;

import io.datareplication.model.Url;
import io.datareplication.model.feed.FeedPageHeader;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;

import java.util.concurrent.CompletionStage;

@AllArgsConstructor(access = AccessLevel.PACKAGE)
class FeedPageCrawler {
    private final HeaderLoader headerLoader;

    @NonNull CompletionStage<@NonNull FeedPageHeader> crawl(@NonNull final Url url,
                                                            @NonNull final StartFrom startFrom) {
        throw new UnsupportedOperationException("NIY");
    }
}
