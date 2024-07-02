package io.datareplication.producer.snapshot;

import io.datareplication.model.Page;
import io.datareplication.model.PageId;
import io.datareplication.model.snapshot.SnapshotEntityHeader;
import io.datareplication.model.snapshot.SnapshotId;
import io.datareplication.model.snapshot.SnapshotPageHeader;
import lombok.NonNull;

import java.util.concurrent.CompletionStage;

/**
 * Repository to store snapshot pages produced by {@link SnapshotProducer}.
 */
public interface SnapshotPageRepository {
    /**
     * Save the given snapshot page in the repository.
     * <p>
     * The combination of snapshot ID and page ID uniquely identifies the page. Page IDs are not necessarily unique
     * across different snapshots.
     * <p>
     * What parts of the page are stored, and in what format, is up to the implementor. The snapshot producer
     * implementation doesn't define a way to retrieve a page from the repository, so implementations are free to
     * store or not store aspects of the page as needed for how they serve pages over HTTP.
     *
     * <h2>Consistency Requirements</h2>
     * When the returned {@link CompletionStage} succeeds, the page must be persisted successfully. If saving the
     * page fails, the returned CompletionStage must also fail.
     *
     * @param snapshotId the ID of the snapshot this page is part of
     * @param pageId the ID of this page
     * @param page the page data
     * @return CompletionStage
     */
    @NonNull CompletionStage<Void> save(@NonNull SnapshotId snapshotId,
                                        @NonNull PageId pageId,
                                        @NonNull Page<@NonNull SnapshotPageHeader, @NonNull SnapshotEntityHeader> page);
}
