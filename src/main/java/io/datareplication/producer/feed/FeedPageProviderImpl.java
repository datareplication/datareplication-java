package io.datareplication.producer.feed;

import io.datareplication.model.Page;
import io.datareplication.model.PageId;
import io.datareplication.model.feed.FeedEntityHeader;
import io.datareplication.model.feed.FeedPageHeader;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;

import java.util.Optional;
import java.util.concurrent.CompletionStage;

@AllArgsConstructor(access = AccessLevel.PACKAGE)
class FeedPageProviderImpl implements FeedPageProvider {
    private final FeedEntityRepository feedEntityRepository;
    private final FeedPageMetadataRepository feedPageMetadataRepository;

    @Override
    public @NonNull CompletionStage<@NonNull Optional<@NonNull PageId>> latestPageId() {
        return feedPageMetadataRepository
            .getWithoutNextLink()
            .thenApply(Generations::selectLatestPage)
            .thenApply(maybeLatestPage -> maybeLatestPage.map(FeedPageMetadataRepository.PageMetadata::pageId));
    }

    @Override
    public @NonNull CompletionStage<@NonNull Optional<@NonNull HeaderWithContentType>> pageHeader(@NonNull PageId id) {
        throw new RuntimeException("not yet implemented");
    }

    @Override
    public @NonNull CompletionStage<@NonNull Optional<@NonNull Page<@NonNull FeedPageHeader, @NonNull FeedEntityHeader>>> page(@NonNull PageId id) {
        throw new RuntimeException("not yet implemented");
    }
}
