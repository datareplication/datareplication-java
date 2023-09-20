package io.datareplication.feed.producer;

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
    }

    @NonNull CompletionStage<@NonNull Optional<@NonNull PageMetadata>> get(@NonNull PageId pageId);

    @NonNull CompletionStage<Void> save(@NonNull List<@NonNull PageMetadata> pages);

    // TODO: was there anything else?
}
