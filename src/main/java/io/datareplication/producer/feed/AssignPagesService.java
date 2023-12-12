package io.datareplication.producer.feed;

import lombok.Value;

import java.util.List;
import java.util.Optional;

class AssignPagesService {
    @Value
    static class AssignPagesResult {
        // These fields are in the order they need to be saved in:
        // ----

        // Entity assignments can happen in bulk because they're not visible without PageMetadata updates.
        List<FeedEntityRepository.PageAssignment> entityPageAssignments;
        // Sometimes we need to rotate the generation on the current latest page before we do anything else.
        Optional<FeedPageMetadataRepository.PageMetadata> newGeneration;
        // The bulk of new pages can be saved in any order because they're not immediately visible.
        List<FeedPageMetadataRepository.PageMetadata> newPages;
        // The new latest page: if this is a new page, it won't be visible because it has a higher generation is higher than the
        // old (technically-still-current) latest page. If this is same latest page as before, this will make all changes
        // visible.
        FeedPageMetadataRepository.PageMetadata newLatestPage;
        // The previous latest page, if it's different. In that case, this is the final step that will make everything visible.
        Optional<FeedPageMetadataRepository.PageMetadata> previousLatestPage;
    }

    Optional<AssignPagesResult> assignPages(Optional<FeedPageMetadataRepository.PageMetadata> maybeLatestPage, List<FeedEntityRepository.PageAssignment> entities) {
        // impl note: if no new pages are created (old latest = new latest) then previousLatestPage must be empty
        // impl note: generation is old latest page generation + 1, and we set that on all pages we return incl the old latest page

        // TODO: impl note: rotating generations happens here now

        throw new UnsupportedOperationException("not yet implemented");
    }
}
