package io.datareplication.consumer.feed;

import io.datareplication.consumer.HttpException;
import io.datareplication.consumer.PageFormatException;
import io.datareplication.consumer.StreamingPage;
import io.datareplication.internal.http.HttpClient;
import io.datareplication.model.Url;
import io.datareplication.model.feed.FeedPageHeader;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;

import java.util.concurrent.CompletionStage;

// TODO: Impl + Tests
@AllArgsConstructor(access = AccessLevel.PACKAGE)
class HeaderLoader {
    private final HttpClient httpClient;

    /**
     * Load the headers of a feed page.
     *
     * @param url the URL to download
     * @return a {@link FeedPageHeader} for the given URL
     * @throws HttpException       in case of HTTP errors (invalid URL, HTTP error status codes,
     *                             network errors/timeouts, ...)
     */
    public @NonNull CompletionStage<@NonNull FeedPageHeader> load(@NonNull Url url) {
        throw new UnsupportedOperationException("NIY");
    }
}
