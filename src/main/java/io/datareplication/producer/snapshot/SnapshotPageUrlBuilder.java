package io.datareplication.producer.snapshot;

import io.datareplication.model.PageId;
import io.datareplication.model.Url;
import io.datareplication.model.snapshot.SnapshotId;
import lombok.NonNull;

/**
 * An interface to build an HTTP url for a specific snapshot page.
 * <p>
 * These are the URLs that consumers will use to download snapshot pages. The returned URLs need to match the
 * routing for your HTTP server that serves the pages.
 * <p>
 * Snapshot page URLs are baked into the snapshot index when the snapshot is produced. This means that changing the
 * format of page URLs will only take effect for snapshots produced from that point onward. You must ensure that
 * old page URL formats continue to work for as long as the old snapshots need to be consumable.
 */
public interface SnapshotPageUrlBuilder {
    /**
     * Return a URL for snapshot page identified by snapshot and page IDs.
     *
     * @param snapshotId the ID of the snapshot
     * @param pageId the page ID
     * @return the public URL of this snapshot page
     */
    @NonNull Url pageUrl(@NonNull SnapshotId snapshotId, @NonNull PageId pageId);
}
