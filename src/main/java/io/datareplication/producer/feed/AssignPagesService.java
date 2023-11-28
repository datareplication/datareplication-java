package io.datareplication.producer.feed;

import lombok.Value;

import java.util.List;
import java.util.Optional;

class AssignPagesService {
    @Value
    static class AssignPagesResult {
        List<FeedEntityRepository.PageAssignment> entityPageAssignments;
        // pages have to be saved in that order: first the bulk of the new pages...
        List<FeedPageMetadataRepository.PageMetadata> newPages;
        // ...then the new latest page (so all pages reachable from there are already available)...
        Optional<FeedPageMetadataRepository.PageMetadata> newLatestPage;
        // ...and then the old latest page which now has the next link, making the new pages reachable
        Optional<FeedPageMetadataRepository.PageMetadata> previousLatestPage;
    }

    AssignPagesResult assignPages(Optional<FeedPageMetadataRepository.PageMetadata> maybeLatestPage, List<FeedEntityRepository.PageAssignment> entities) {
        // impl note: if no new pages are created (i.e. the old latest page is the new latest page)
        // we have to make sure to store it only in one of the fields to avoid problems
        // impl note: generation is old latest page generation + 1, and we set that on all pages we return incl the old latest page

        throw new UnsupportedOperationException("not yet implemented");
    }
}
