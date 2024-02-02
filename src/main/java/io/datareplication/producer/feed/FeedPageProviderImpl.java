package io.datareplication.producer.feed;

import io.datareplication.internal.multipart.MultipartUtils;
import io.datareplication.model.ContentType;
import io.datareplication.model.Page;
import io.datareplication.model.PageId;
import io.datareplication.model.feed.FeedEntityHeader;
import io.datareplication.model.feed.FeedPageHeader;
import io.datareplication.model.feed.Link;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import reactor.core.publisher.Mono;

import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

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
                var boundary = MultipartUtils.defaultBoundary(page.pageId());
                var contentType = MultipartUtils.pageContentType(boundary);
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
        var pageMetadataFuture = Mono.fromCompletionStage(() -> feedPageMetadataRepository.get(id));
        var entitiesFuture = Mono.fromCompletionStage(() -> feedEntityRepository.get(id));
        return Mono
            .zip(pageMetadataFuture, entitiesFuture)
            .map(args -> {
                var entities = args.getT2();
                return args.getT1().map(page -> {
                    // TODO: refactor to somewhere else
                    var boundary = MultipartUtils.defaultBoundary(page.pageId());
                    var header = new FeedPageHeader(
                            page.lastModified(),
                            Link.self(feedPageUrlBuilder.pageUrl(page.pageId())),
                            page.prev().map(x -> Link.prev(feedPageUrlBuilder.pageUrl(x))),
                            page.next().map(x -> Link.next(feedPageUrlBuilder.pageUrl(x)))
                        );
                    var entitiesList = entities.stream().limit(page.numberOfEntities()).collect(Collectors.toList());
                    //entities.subList(0, page.numberOfEntities())
                    return new Page<>(
                        header,
                        boundary,
                        entitiesList
                    );
                });
            })
            .toFuture();
    }
}
