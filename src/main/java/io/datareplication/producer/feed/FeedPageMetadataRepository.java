package io.datareplication.producer.feed;

import io.datareplication.model.PageId;
import io.datareplication.model.Timestamp;
import lombok.NonNull;
import lombok.Value;

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

    @NonNull CompletionStage<Void> save(@NonNull List<@NonNull PageMetadata> pages);

    // TODO: explain: page with no next link with the lowest generation
    // TODO: implementation note: need to rotate generations once we reach a limit
    @NonNull CompletionStage<@NonNull Optional<@NonNull PageMetadata>> getLatest();
}
