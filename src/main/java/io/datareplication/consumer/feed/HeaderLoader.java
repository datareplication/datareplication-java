package io.datareplication.consumer.feed;

import io.datareplication.internal.http.HttpClient;
import io.datareplication.model.Url;
import io.datareplication.model.feed.FeedPageHeader;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import reactor.core.publisher.Mono;

// TODO: Impl + Tests
@AllArgsConstructor(access = AccessLevel.PACKAGE)
class HeaderLoader {
    private final HttpClient httpClient;

    /**
     * Load the headers of a feed page.
     *
     * @param url the URL to download
     * @return a {@link FeedPageHeader} for the given URL
     */
    public @NonNull Mono<@NonNull FeedPageHeader> load(@NonNull Url url) {
        return Mono.error(new UnsupportedOperationException("NIY"));
    }
}
