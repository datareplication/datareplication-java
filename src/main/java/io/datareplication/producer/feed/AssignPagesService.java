package io.datareplication.producer.feed;

import io.datareplication.model.PageId;
import io.datareplication.model.Timestamp;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Value;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Getter
@AllArgsConstructor
final class MutablePage {
    private final PageId pageId;
    private Timestamp lastModified;
    private final Optional<PageId> prev;
    private long contentLength;
    private int numberOfEntities;

    static MutablePage fromLoadedPage(FeedPageMetadataRepository.PageMetadata page) {
        return new MutablePage(
            page.pageId(),
            page.lastModified(),
            page.prev(),
            page.numberOfBytes(),
            page.numberOfEntities()
        );
    }

    static MutablePage emptyPage(PageId pageId) {
        return new MutablePage(
            pageId,
            Timestamp.of(Instant.EPOCH),
            Optional.empty(),
            0,
            0
        );
    }

    static MutablePage emptyPage(PageId pageId, PageId prev) {
        return new MutablePage(
            pageId,
            Timestamp.of(Instant.EPOCH),
            Optional.of(prev),
            0,
            0
        );
    }

    void append(FeedEntityRepository.PageAssignment entity) {
        lastModified = entity.lastModified();
        contentLength += entity.contentLength();
        numberOfEntities += 1;
    }

    FeedPageMetadataRepository.PageMetadata finishPage(Optional<PageId> next, int generation) {
        return new FeedPageMetadataRepository.PageMetadata(
            pageId,
            lastModified,
            prev,
            next,
            contentLength,
            numberOfEntities,
            generation
        );
    }
}

@AllArgsConstructor(access = AccessLevel.PACKAGE)
class AssignPagesService {
    private final RandomPageIdProvider pageIdProvider;
    private final long maxBytesPerPage;
    private final long maxEntitiesPerPage;

    @Value
    static class AssignPagesResult {
        // These fields are in the order they need to be saved in:
        // ----

        // Entity assignments can happen in bulk because they're not visible without PageMetadata updates.
        List<FeedEntityRepository.PageAssignment> entityPageAssignments;
        // The bulk of new pages can be saved in any order because they're not immediately visible.
        List<FeedPageMetadataRepository.PageMetadata> newPages;
        // The new latest page: if this is a new page, it won't be visible because it has a higher generation is higher
        // than the old (technically-still-current) latest page. If this is same latest page as before, this will make
        // all changes visible.
        FeedPageMetadataRepository.PageMetadata newLatestPage;
        // The previous latest page, if it's different. In that case, this is the final step that will make everything
        // visible.
        Optional<FeedPageMetadataRepository.PageMetadata> previousLatestPage;
    }

    Optional<AssignPagesResult> assignPages(
        Optional<FeedPageMetadataRepository.PageMetadata> maybeLatestPage,
        List<FeedEntityRepository.PageAssignment> unassignedEntities
    ) {
        if (unassignedEntities.isEmpty()) {
            return Optional.empty();
        }

        final int generation = maybeLatestPage
            .map(previousLatestPage -> previousLatestPage.generation() + 1)
            .orElse(Generations.INITIAL_GENERATION);

        final var assignedEntities = new ArrayList<FeedEntityRepository.PageAssignment>();
        final var newPages = new ArrayList<FeedPageMetadataRepository.PageMetadata>();
        var previousLatestPage = Optional.<FeedPageMetadataRepository.PageMetadata>empty();

        // we start with either the old latest page or a new one as the "current" page
        var currentPage = maybeLatestPage
            .map(MutablePage::fromLoadedPage)
            .orElseGet(() -> MutablePage.emptyPage(pageIdProvider.newPageId()));
        for (var entity : unassignedEntities) {
            if (!fitsOnPage(currentPage, entity)) {
                // if this entity doesn't fit on the current page, we close it and create a new one
                var next = pageIdProvider.newPageId();
                var finalizedPage = currentPage.finishPage(Optional.of(next), generation);
                currentPage = MutablePage.emptyPage(next, finalizedPage.pageId());

                if (maybeLatestPage.stream().anyMatch(p -> p.pageId().equals(finalizedPage.pageId()))) {
                    // if this page has the same ID as the old latest page, we have to store it separately
                    previousLatestPage = Optional.of(finalizedPage);
                } else {
                    // just add it to the pile
                    newPages.add(finalizedPage);
                }
            }

            // update the current page metadata
            currentPage.append(entity);
            assignedEntities.add(assignedEntity(entity, currentPage.pageId()));
        }

        return Optional.of(new AssignPagesResult(
            assignedEntities,
            newPages,
            currentPage.finishPage(Optional.empty(), generation),
            previousLatestPage
        ));
    }

    private boolean fitsOnPage(MutablePage page, FeedEntityRepository.PageAssignment entity) {
        var pageEmpty = page.numberOfEntities() == 0;
        var wouldBeTooManyEntities = page.numberOfEntities() + 1 > maxEntitiesPerPage;
        var wouldBeTooManyBytes = page.contentLength() + entity.contentLength() > maxBytesPerPage;
        return pageEmpty || (!wouldBeTooManyEntities && !wouldBeTooManyBytes);
    }

    private static FeedEntityRepository.PageAssignment assignedEntity(
        FeedEntityRepository.PageAssignment entity,
        PageId pageId
    ) {
        return new FeedEntityRepository.PageAssignment(
            entity.contentId(),
            entity.lastModified(),
            entity.originalLastModified(),
            entity.contentLength(),
            Optional.of(pageId)
        );
    }
}
