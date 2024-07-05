package io.datareplication.producer.snapshot;

import io.datareplication.model.snapshot.SnapshotIndex;
import lombok.NonNull;

import java.util.concurrent.CompletionStage;

/**
 * Repository to store a {@link SnapshotIndex} created by the snapshot consumer.
 */
public interface SnapshotIndexRepository {
    /**
     * Save the given {@link SnapshotIndex} in the repository.
     * <p>
     * When this method is called, all pages that are part of the snapshot will have already been saved in the
     * configured {@link SnapshotPageRepository}. Once the index is saved, the snapshot is done and can be served to
     * consumers.
     *
     * @param snapshotIndex the snapshot index to store
     * @return CompletionStage
     */
    @NonNull
    CompletionStage<Void> save(@NonNull SnapshotIndex snapshotIndex);
}
