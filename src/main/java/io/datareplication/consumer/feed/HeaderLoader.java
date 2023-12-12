package io.datareplication.consumer.feed;

import io.datareplication.internal.http.HttpClient;
import io.datareplication.model.Url;
import io.datareplication.model.feed.FeedPageHeader;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;

import java.util.concurrent.CompletionStage;

// TODO: Impl + Tests
@AllArgsConstructor(access = AccessLevel.PACKAGE)
public class HeaderLoader {
    private final HttpClient httpClient;

    public @NonNull CompletionStage<@NonNull FeedPageHeader> load(@NonNull Url url) {
        throw new UnsupportedOperationException("NIY");
    }
}
