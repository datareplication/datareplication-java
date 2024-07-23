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
 * This repository always stores either zero or one records.
 *
 * <h2>Consistency Requirements</h2>
 * In addition to {@link io.datareplication.producer.feed the common requirements for all feed producer repositories},
 * all writes to this repository must be atomic: {@link #get()} may never observe a partially written or deleted
 * state.
 */
public interface FeedProducerJournalRepository {
    /**
     * The rollback information stored by the feed producer to allow clean rollbacks. The repository implementation
     * must save and load all fields accurately.
     */
    @Value
    class JournalState {
        /**
         * List of IDs of pages that were created by the currently active transaction.
         */
        @NonNull
        List<@NonNull PageId> newPages;
        /**
         * ID of the latest feed page when the active transaction completes.
         */
        @NonNull
        PageId newLatestPage;
        /**
         * ID of the latest feed page at the start of the transaction, if different from {@link #newLatestPage()}.
         */
        @NonNull
        Optional<@NonNull PageId> previousLatestPage;
    }

    /**
     * Save the given journal state to the repository.
     * <p>
     * This replaces the current entry, if any.
     *
     * @param state the entry to save
     * @return CompletionStage
     */
    @NonNull
    CompletionStage<Void> save(@NonNull JournalState state);

    /**
     * Get the currently stored journal state from the repository.
     * <p>
     * This should either return the state passed to the most recent call to {@link #save(JournalState)} or
     * {@link Optional#empty()} if {@link #delete()} was called most recently or {@link #save(JournalState)}
     * was never called
     *
     * @return CompletionStage of the currently stored journal state
     */
    @NonNull
    CompletionStage<@NonNull Optional<@NonNull JournalState>> get();

    /**
     * Delete the currently stored journal state from the repository.
     * <p>
     * If there's no current entry (i.e. whenever {@link #get()} returns nothing), this method is a no-op.
     *
     * @return CompletionStage
     */
    @NonNull
    CompletionStage<Void> delete();
}
