package io.datareplication.consumer.feed;

import io.datareplication.model.Url;
import io.datareplication.model.feed.FeedPageHeader;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import reactor.core.publisher.Mono;

@AllArgsConstructor(access = AccessLevel.PACKAGE)
class FeedPageCrawler {
    private final HeaderLoader headerLoader;

    @NonNull Mono<@NonNull FeedPageHeader> crawl(
        @NonNull final Url url,
        @NonNull final StartFrom startFrom
    ) {
        return beginCrawl(url, startFrom);
    }

    private @NonNull Mono<@NonNull FeedPageHeader> beginCrawl(
        @NonNull final Url url,
        @NonNull final StartFrom startFrom
    ) {

        // TODO: Implement other StartFrom types (StartFrom.Timestamp, StartFrom.ContentId)
        var currentPageHeader = headerLoader.load(url);

        return currentPageHeader
            .map(FeedPageHeader::prev)
            .flatMap(optionalPrev -> optionalPrev
                .map(prev -> beginCrawl(prev.value(), startFrom))
                .orElse(currentPageHeader)
            );
    }
}
