package io.datareplication.producer.feed;

import io.datareplication.model.PageId;
import lombok.NonNull;
import lombok.Value;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

public interface FeedPageMetadataRepository {
    @Value
    class PageMetadata {
        @NonNull PageId pageId;
        @NonNull Instant lastModified;
        @NonNull Optional<@NonNull PageId> prev;
        @NonNull Optional<@NonNull PageId> next;
        long numberOfBytes;
        int numberOfEntities;
        int generation;
    }

    @NonNull CompletionStage<@NonNull Optional<@NonNull PageMetadata>> get(@NonNull PageId pageId);

    @NonNull CompletionStage<@NonNull List<@NonNull PageMetadata>> getWithoutNextLink();

    @NonNull CompletionStage<Void> save(@NonNull List<@NonNull PageMetadata> pages);

    @NonNull CompletionStage<Void> delete(@NonNull List<@NonNull PageId> pageIds);
}

