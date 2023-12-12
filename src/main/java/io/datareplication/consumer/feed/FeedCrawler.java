package io.datareplication.consumer.feed;

import io.datareplication.internal.http.HttpClient;
import io.datareplication.model.Url;
import io.datareplication.model.feed.FeedPageHeader;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;

import java.util.concurrent.CompletionStage;

@AllArgsConstructor(access = AccessLevel.PACKAGE)
public class FeedCrawler {
    private final HttpClient httpClient;

    @NonNull CompletionStage<@NonNull FeedPageHeader> crawl(@NonNull final Url url,
                                                            @NonNull final StartFrom startFrom) {
        throw new UnsupportedOperationException("NIY");
    }
}
