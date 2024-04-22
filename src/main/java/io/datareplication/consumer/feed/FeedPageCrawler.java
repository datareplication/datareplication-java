package io.datareplication.consumer.feed;

import io.datareplication.consumer.CrawlingException;
import io.datareplication.consumer.PageFormatException;
import io.datareplication.model.Timestamp;
import io.datareplication.model.Url;
import io.datareplication.model.feed.FeedPageHeader;
import io.datareplication.model.feed.Link;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * A crawler which searches for the {@link Url} by making head requests to the feed pages
 * and recursively following the prev links until the start {@link Url} is found.
 */
@AllArgsConstructor(access = AccessLevel.PACKAGE)
class FeedPageCrawler {
    private final HeaderLoader headerLoader;

    /**
     * Crawls the feed page starting from the given {@link Url}.
     *
     * @param url       the {@link Url} to start crawling from
     * @param startFrom the {@link StartFrom} parameter
     * @return the {@link Url} to start streaming from
     * @throws CrawlingException if the last modified date of the last page is not older
     *                           and {@link StartFrom} is not {@link StartFrom.Beginning
     */
    @NonNull
    Mono<@NonNull Url> crawl(
        @NonNull final Url url,
        @NonNull final StartFrom startFrom
    ) {
        var currentPageHeader = headerLoader.load(url);
        return currentPageHeader
            .flatMap(pageHeader ->
                Objects.requireNonNullElseGet(
                    returnStartUrlIfMatchingToStartFrom(pageHeader, startFrom),
                    () -> pageHeader
                        .prev()
                        .map(prev -> crawl(prev.value(), startFrom))
                        .orElse(
                            currentPageHeader.handle((header, sink) -> {
                                    if (startFrom instanceof StartFrom.Timestamp) {
                                        sink.error(new CrawlingException(
                                            header.self().value(),
                                            header.lastModified(),
                                            ((StartFrom.Timestamp) startFrom).timestamp()
                                        ));
                                    } else if (startFrom instanceof StartFrom.ContentId) {
                                        sink.error(new CrawlingException(
                                            header.self().value(),
                                            header.lastModified(),
                                            ((StartFrom.ContentId) startFrom).timestamp()
                                        ));
                                    } else {
                                        sink.next(header.self().value());
                                    }
                                }
                            )
                        )
                )
            );
    }

    private static Mono<Url> returnStartUrlIfMatchingToStartFrom(
        final @NonNull FeedPageHeader pageHeader,
        final @NonNull StartFrom startFrom
    ) {
        if (startFrom instanceof StartFrom.Timestamp
            && isFeedPageLastModifiedBefore(pageHeader, ((StartFrom.Timestamp) startFrom).timestamp())) {
            return Mono.just(nextLinkUrlFromFeedPageHeader(pageHeader));
        } else if (startFrom instanceof StartFrom.ContentId
            && isFeedPageLastModifiedBefore(pageHeader, ((StartFrom.ContentId) startFrom).timestamp())) {
            return Mono.just(nextLinkUrlFromFeedPageHeader(pageHeader));
        } else {
            return null;
        }
    }

    private static boolean isFeedPageLastModifiedBefore(final FeedPageHeader pageHeader, final Timestamp startFrom) {
        return pageHeader
            .lastModified()
            .isBefore(startFrom);
    }

    private static Url nextLinkUrlFromFeedPageHeader(final FeedPageHeader pageHeader) {
        return pageHeader
            .next()
            .map(Link.Next::value)
            .orElseThrow(() -> new PageFormatException.MissingNextLinkHeader(pageHeader.toHttpHeaders()));
    }
}
