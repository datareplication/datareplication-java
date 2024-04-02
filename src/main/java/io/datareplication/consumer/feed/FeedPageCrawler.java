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
    @NonNull Mono<@NonNull Url> crawl(
        @NonNull final Url url,
        @NonNull final StartFrom startFrom
    ) {
        var currentPageHeader = headerLoader.load(url);
        return currentPageHeader
            .flatMap(pageHeader -> {
                var nullableStartUrl = nullableStartUrl(pageHeader, startFrom);
                return Objects.requireNonNullElseGet(nullableStartUrl, () -> pageHeader
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

    private static Mono<@NonNull Url> nullableStartUrl(
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
