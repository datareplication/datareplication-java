package io.datareplication.feed.producer;

import io.datareplication.model.Entity;
import io.datareplication.model.PageId;
import io.datareplication.model.Timestamp;
import io.datareplication.model.feed.ContentId;
import io.datareplication.model.feed.FeedEntityHeader;
import lombok.NonNull;
import lombok.Value;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

// TODO: interface is messy
public interface FeedEntityRepository {
    @Value
    class PageAssignment {
        @NonNull ContentId contentId;
        @NonNull Timestamp lastModified;
        @NonNull int contentLength;
        @NonNull Optional<@NonNull PageId> pageId;
    }

    @NonNull CompletionStage<Void> append(@NonNull Entity<@NonNull FeedEntityHeader> entity);

    @NonNull CompletionStage<@NonNull List<@NonNull Entity<@NonNull FeedEntityHeader>>> get(@NonNull PageId pageId);

    @NonNull CompletionStage<@NonNull List<@NonNull PageAssignment>> getUnassigned();

    @NonNull CompletionStage<@NonNull List<@NonNull PageAssignment>> getPageAssignments(@NonNull PageId pageId);

    @NonNull CompletionStage<Void> savePageAssignments(@NonNull List<@NonNull PageAssignment> pageAssignments);
}
