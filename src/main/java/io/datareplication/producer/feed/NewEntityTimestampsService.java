package io.datareplication.producer.feed;

import java.util.List;

class NewEntityTimestampsService {
    // TODO: don't love the name
    List<FeedEntityRepository.PageAssignment> updatedEntityTimestamps(FeedPageMetadataRepository.PageMetadata latestPage,
                                                                      List<FeedEntityRepository.PageAssignment> entities) {
        // TODO: actually maybe we have to make the timestamping rollbackable to avoid reorderings if page assignments
        //  are rolled back but the timestamp changes remain?
        //  Plan: if we update lastModified, save the original value in originalLastModified. Then rollback can just
        //  undo that together with unsetting the page id
        throw new UnsupportedOperationException("not yet implemented");
    }
}
