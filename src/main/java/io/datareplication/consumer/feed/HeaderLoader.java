package io.datareplication.consumer.feed;

import io.datareplication.consumer.PageFormatException;
import io.datareplication.internal.http.HttpClient;
import io.datareplication.model.HttpHeader;
import io.datareplication.model.HttpHeaders;
import io.datareplication.model.Url;
import io.datareplication.model.feed.FeedPageHeader;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import reactor.core.publisher.Mono;

import java.net.http.HttpResponse;
import java.util.stream.Stream;

@AllArgsConstructor(access = AccessLevel.PACKAGE)
class HeaderLoader {
    private final HttpClient httpClient;
    private final FeedPageHeaderParser feedPageHeaderParser;

    /**
     * Load the headers of a feed page.
     *
     * @param url the URL to download
     * @return a {@link FeedPageHeader} for the given URL
     * @throws PageFormatException if the headers are malformed or missing
     */
    public @NonNull Mono<@NonNull FeedPageHeader> load(@NonNull Url url) {
        return httpClient
            .head(url)
            .map(response -> feedPageHeaderParser.feedPageHeader(convertHeaders(response)));
    }

    private @NonNull HttpHeaders convertHeaders(@NonNull HttpResponse<?> response) {
        final Stream<HttpHeader> headers = response
            .headers()
            .map()
            .entrySet()
            .stream()
            .map(entry -> HttpHeader.of(entry.getKey(), entry.getValue()));
        return HttpHeaders.of(headers.iterator());
    }
}
