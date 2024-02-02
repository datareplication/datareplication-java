package io.datareplication.producer.feed;

import io.datareplication.internal.multipart.MultipartUtils;
import io.datareplication.model.Entity;
import io.datareplication.model.Page;
import io.datareplication.model.PageId;
import io.datareplication.model.feed.FeedEntityHeader;
import io.datareplication.model.feed.FeedPageHeader;
import io.datareplication.model.feed.Link;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import reactor.core.publisher.Mono;

import java.util.List;
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
            .thenApply(maybePageMetadata -> maybePageMetadata.map(this::headerWithContentType));
    }

    @Override
    public @NonNull CompletionStage<@NonNull Optional<@NonNull Page<@NonNull FeedPageHeader, @NonNull FeedEntityHeader>>> page(@NonNull PageId id) {
        var pageMetadataFuture = feedPageMetadataRepository
            .get(id);
        var entitiesFuture = feedEntityRepository.get(id);
        return pageMetadataFuture.thenCombine(entitiesFuture, (maybePageMetadata, entities) ->
            maybePageMetadata.map(pageMetadata -> page(pageMetadata, entities))
        );
    }

    private FeedPageHeader feedPageHeader(FeedPageMetadataRepository.PageMetadata pageMetadata) {
        return new FeedPageHeader(
            pageMetadata.lastModified(),
            Link.self(feedPageUrlBuilder.pageUrl(pageMetadata.pageId())),
            pageMetadata
                .prev()
                .map(feedPageUrlBuilder::pageUrl)
                .map(Link::prev),
            pageMetadata
                .next()
                .map(feedPageUrlBuilder::pageUrl)
                .map(Link::next)
        );
    }

    private HeaderWithContentType headerWithContentType(FeedPageMetadataRepository.PageMetadata pageMetadata) {
        var boundary = MultipartUtils.defaultBoundary(pageMetadata.pageId());
        return new HeaderWithContentType(
            feedPageHeader(pageMetadata),
            MultipartUtils.pageContentType(boundary)
        );
    }

    private Page<FeedPageHeader, FeedEntityHeader> page(
        FeedPageMetadataRepository.PageMetadata pageMetadata,
        List<Entity<FeedEntityHeader>> entities
    ) {
        return new Page<>(
            feedPageHeader(pageMetadata),
            MultipartUtils.defaultBoundary(pageMetadata.pageId()),
            entities.subList(0, pageMetadata.numberOfEntities())
        );
    }
}
