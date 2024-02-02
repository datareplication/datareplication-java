package io.datareplication.producer.feed;

import io.datareplication.model.ContentType;
import io.datareplication.model.Page;
import io.datareplication.model.PageId;
import io.datareplication.model.feed.FeedEntityHeader;
import io.datareplication.model.feed.FeedPageHeader;
import io.datareplication.model.feed.Link;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;

import java.util.Optional;
import java.util.concurrent.CompletionStage;

@AllArgsConstructor(access = AccessLevel.PACKAGE)
class FeedPageProviderImpl implements FeedPageProvider {
    private final FeedEntityRepository feedEntityRepository;
    private final FeedPageMetadataRepository feedPageMetadataRepository;
    private final FeedPageUrlBuilder feedPageUrlBuilder;

    @Override
    public @NonNull CompletionStage<@NonNull Optional<@NonNull PageId>> latestPageId() {
        return feedPageMetadataRepository
            .getWithoutNextLink()
            .thenApply(Generations::selectLatestPage)
            .thenApply(maybeLatestPage -> maybeLatestPage.map(FeedPageMetadataRepository.PageMetadata::pageId));
    }

    @Override
    public @NonNull CompletionStage<@NonNull Optional<@NonNull HeaderWithContentType>> pageHeader(@NonNull PageId id) {
        return feedPageMetadataRepository
            .get(id)
            .thenApply(maybePage -> maybePage.map(page -> {
                // TODO: refactor to somewhere else
                var boundary = String.format("_---_%s", page.pageId().value());
                var contentType = ContentType.of(String.format("multipart/mixed; boundary=\"%s\"", boundary));
                return new HeaderWithContentType(
                    new FeedPageHeader(
                        page.lastModified(),
                        Link.self(feedPageUrlBuilder.pageUrl(page.pageId())),
                        page.prev().map(x -> Link.prev(feedPageUrlBuilder.pageUrl(x))),
                        page.next().map(x -> Link.next(feedPageUrlBuilder.pageUrl(x)))
                    ),
                    contentType
                );
            }));
    }

    @Override
    public @NonNull CompletionStage<@NonNull Optional<@NonNull Page<@NonNull FeedPageHeader, @NonNull FeedEntityHeader>>> page(@NonNull PageId id) {
        throw new RuntimeException("not yet implemented");
    }
}
