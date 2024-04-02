package io.datareplication.consumer.feed;

import io.datareplication.consumer.CrawlingException;
import io.datareplication.model.Timestamp;
import io.datareplication.model.Url;
import io.datareplication.model.feed.FeedPageHeader;
import io.datareplication.model.feed.Link;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import reactor.core.publisher.Mono;

import java.util.Objects;

@AllArgsConstructor(access = AccessLevel.PACKAGE)
class FeedPageCrawler {
    private final HeaderLoader headerLoader;

    /**
     * Crawls the feed page starting from the given URL.
     *
     * @param url       the URL to start crawling from
     * @param startFrom the StartFrom parameter
     * @return the last URL of the feed page
     * @throws CrawlingException if the last modified date of the last feed page
     *                           is not older than the StartFrom timestamp
     */
    @NonNull Mono<@NonNull Url> crawl(
        @NonNull final Url url,
        @NonNull final StartFrom startFrom
    ) {
        var currentPageHeader = headerLoader.load(url);
        return currentPageHeader
            .flatMap(pageHeader -> {
                var startingFeedPageHeader = hasStartUrl(pageHeader, startFrom);
                return Objects.requireNonNullElseGet(startingFeedPageHeader, () -> pageHeader
                    .prev()
                    .map(prev -> crawl(prev.value(), startFrom))
                    .orElse(currentPageHeader.map(header -> returnLastUrl(startFrom, header))));
            });
    }

    private static @NonNull Url returnLastUrl(
        final @NonNull StartFrom startFrom,
        final @NonNull FeedPageHeader feedPageHeader
    ) {
        if (startFrom instanceof StartFrom.Timestamp) {
            throw new CrawlingException(
                feedPageHeader.self().value(),
                feedPageHeader.lastModified(),
                ((StartFrom.Timestamp) startFrom).timestamp()
            );
        } else if (startFrom instanceof StartFrom.ContentId) {
            throw new CrawlingException(
                feedPageHeader.self().value(),
                feedPageHeader.lastModified(),
                ((StartFrom.ContentId) startFrom).timestamp()
            );
        } else {
            return feedPageHeader.self().value();
        }
    }

    private static Mono<@NonNull Url> hasStartUrl(
        final @NonNull FeedPageHeader pageHeader,
        final @NonNull StartFrom startFrom
    ) {
        if (startFrom instanceof StartFrom.Timestamp
            && isBefore(pageHeader, ((StartFrom.Timestamp) startFrom).timestamp())) {
            return getNextLinkUrl(pageHeader);
        } else if (startFrom instanceof StartFrom.ContentId
            && isBefore(pageHeader, ((StartFrom.ContentId) startFrom).timestamp())) {
            return getNextLinkUrl(pageHeader);
        } else {
            return null;
        }
    }

    private static boolean isBefore(final FeedPageHeader pageHeader, final Timestamp startFrom) {
        return pageHeader.lastModified().isBefore(startFrom);
    }

    private static Mono<Url> getNextLinkUrl(final FeedPageHeader pageHeader) {
        return pageHeader.next().map(Link.Next::value).map(Mono::just).orElse(null);
    }
}
