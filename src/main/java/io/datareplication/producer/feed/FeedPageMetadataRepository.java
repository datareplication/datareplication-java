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

    // TODO: I still don't love this method; it has some potentially tricky requirements around using a
    //  delayed-update index: in principle the newly-saved page needs to be marked as a candidate (updating the index),
    //  then the pages need to be updated, then the old page needs to be un-marked (again updating the index)
    //  I think this is the only really tricky query: get in this class only goes by primary key
    //  It might be enough to filter anything that has a next link in Generations.selectLatestPage?
    //  delayed index update:
    //  * ok when saving the new page because it doesn't matter
    //  * ok when saving the old page because once the link appears, we ignore and use the new page
    //  preemptive index update:
    //  * doesn't really make sense when saving the new page because it doesn't exist yet? so the repo can't sensibly
    //    return it
    //  * NOT ok when saving the old page because it disappears, switching the latest page, before the old page has
    //    been updated; probably fine in practice because you'll get stuck on the old latest page because the next
    //    link isn't set, so at worst it's like an awkward consumer experience?
    //  so TL;DR it's fine to keep a separately updated index for this query, but if so it must be updated *after* the
    //  actual record has been written?
    @NonNull CompletionStage<@NonNull List<@NonNull PageMetadata>> getWithoutNextLink();

    @NonNull CompletionStage<Void> save(@NonNull List<@NonNull PageMetadata> pages);

    @NonNull CompletionStage<Void> delete(@NonNull List<@NonNull PageId> pageIds);
}

