package io.datareplication.consumer.feed;

import io.datareplication.model.Url;
import io.datareplication.model.feed.FeedPageHeader;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import reactor.core.publisher.Mono;

import java.util.concurrent.CompletionStage;

@AllArgsConstructor(access = AccessLevel.PACKAGE)
class FeedPageCrawler {
    private final HeaderLoader headerLoader;

    @NonNull CompletionStage<@NonNull FeedPageHeader> crawl(
        @NonNull final Url url,
        @NonNull final StartFrom startFrom
    ) {
        return beginCrawl(url, startFrom);
    }

    private @NonNull CompletionStage<@NonNull FeedPageHeader> beginCrawl(
        @NonNull final Url url,
        @NonNull final StartFrom startFrom
    ) {
        // TODO: Implement other StartFrom types (StartFrom.Timestamp, StartFrom.ContentId)
        var currentPage = Mono.fromCompletionStage(headerLoader.load(url));

        return currentPage
            .map(FeedPageHeader::prev)
            .flatMap(optionalPrev ->
                optionalPrev
                    .map(prev ->
                        Mono.fromCompletionStage(beginCrawl(prev.value(), startFrom))).orElse(currentPage))
            .toFuture();
    }
}
