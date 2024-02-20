package io.datareplication.producer.feed;

import io.datareplication.model.PageId;
import lombok.NonNull;
import lombok.Value;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

public interface FeedProducerJournalRepository {
    @Value
    class JournalState {
        @NonNull List<@NonNull PageId> newPages;
        @NonNull PageId newLatestPage;
        @NonNull Optional<@NonNull PageId> previousLatestPage;
    }

    @NonNull CompletionStage<Void> save(@NonNull JournalState state);

    @NonNull CompletionStage<@NonNull Optional<@NonNull JournalState>> get();

    @NonNull CompletionStage<Void> delete();
}
