package io.datareplication.producer.feed;

import io.datareplication.model.PageId;
import io.datareplication.model.Timestamp;
import lombok.NonNull;
import lombok.Value;
import reactor.core.publisher.Mono;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

public interface FeedPageMetadataRepository {
    @Value
    class PageMetadata {
        @NonNull PageId pageId;
        @NonNull Timestamp lastModified;
        @NonNull Optional<@NonNull PageId> prev;
        @NonNull Optional<@NonNull PageId> next;
        long contentLength;
        int numberOfEntities;
        int generation;
    }

    @NonNull CompletionStage<@NonNull Optional<@NonNull PageMetadata>> get(@NonNull PageId pageId);

    @NonNull CompletionStage<@NonNull List<@NonNull PageMetadata>> getWithoutNextLink();

    @NonNull CompletionStage<Void> save(@NonNull List<@NonNull PageMetadata> pages);

    @NonNull CompletionStage<Void> delete(@NonNull List<@NonNull PageId> pageIds);
}

class FeedPageMetadataRepositoryActions {
    private FeedPageMetadataRepositoryActions() {
    }

    /**
     * The "latest page" in a repository is the page with no next link with the lowest generation. This is implemented as
     * a helper function here so repository implementors don't have to understand generations.
     */
    static Mono<Optional<FeedPageMetadataRepository.PageMetadata>> getLatest(FeedPageMetadataRepository repository) {
        // it just looks bad here
        //noinspection Convert2MethodRef
        return Mono
            .fromCompletionStage(repository::getWithoutNextLink)
            .map(candidates -> candidates.stream().min(Comparator.comparingInt(page -> page.generation())));
    }
}
