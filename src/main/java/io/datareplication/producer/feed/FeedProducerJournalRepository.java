package io.datareplication.producer.feed;

import io.datareplication.model.PageId;
import lombok.NonNull;
import lombok.Value;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

/**
 * Repository to store rollback information for the feed producer.
 * <p>
 * The feed producer uses this information to cleanly roll back partial changes in case of a crash or other
 * interruption. The exact meaning of the stored fields is an implementation detail and might change
 * (in a compatible way). The repository doesn't need to know what the fields mean, it just needs to save and load
 * their values accurately.
 * <p>
 * This repository always stores either zero or one records, so primary keys or indices are unnecessary.
 *
 * <h2>Consistency Requirements</h2>
 * When the
 */
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
