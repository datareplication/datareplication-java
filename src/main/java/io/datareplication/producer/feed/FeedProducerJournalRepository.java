package io.datareplication.producer.feed;

import io.datareplication.model.PageId;
import lombok.NonNull;

import java.util.List;
import java.util.concurrent.CompletionStage;

public interface FeedProducerJournalRepository {
    @NonNull CompletionStage<Void> setInProgressPages(@NonNull List<@NonNull PageId> pageIds);

    @NonNull CompletionStage<@NonNull List<@NonNull PageId>> getInProgressPages();
}
