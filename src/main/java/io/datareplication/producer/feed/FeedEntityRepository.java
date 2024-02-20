package io.datareplication.producer.feed;

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
    // TODO: better name?
    // TODO: split in two (output and update) to not make it seem like contentLength (and what else?) can ever change?
    @Value
    class PageAssignment {
        @NonNull ContentId contentId;
        @NonNull Timestamp lastModified;
        @NonNull Optional<@NonNull Timestamp> originalLastModified;
        long contentLength;
        @NonNull Optional<@NonNull PageId> pageId;
    }

    // TODO: clearer connection between entity and PageAssignment? Maybe a wrapper type that contains all?
    @NonNull CompletionStage<Void> append(@NonNull Entity<@NonNull FeedEntityHeader> entity);

    // TODO: doc: entities must be sorted by (timestamp, disambiguator) ascending
    @NonNull CompletionStage<@NonNull List<@NonNull Entity<@NonNull FeedEntityHeader>>> get(@NonNull PageId pageId);

    // TODO: doc: entities must be sorted by (timestamp, disambiguator) ascending
    @NonNull CompletionStage<@NonNull List<@NonNull PageAssignment>> getUnassigned(int limit);

    // TODO: doc: entities must be sorted by (timestamp, disambiguator) ascending
    @NonNull CompletionStage<@NonNull List<@NonNull PageAssignment>> getPageAssignments(@NonNull PageId pageId);

    @NonNull CompletionStage<Void> savePageAssignments(@NonNull List<@NonNull PageAssignment> pageAssignments);
}
