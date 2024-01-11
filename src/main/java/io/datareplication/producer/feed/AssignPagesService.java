package io.datareplication.producer.feed;

import io.datareplication.model.PageId;
import io.datareplication.model.Timestamp;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Data
@AllArgsConstructor
final class MutablePage {
    final PageId pageId;
    Timestamp lastModified;
    final Optional<PageId> prev;
    long contentLength;
    int numberOfEntities;

    static MutablePage emptyPage(PageId pageId, Optional<PageId> prev) {
        return new MutablePage(
            pageId,
            Timestamp.of(Instant.EPOCH),
            prev,
            0,
            0
        );
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
        // The new latest page: if this is a new page, it won't be visible because it has a higher generation is higher than the
        // old (technically-still-current) latest page. If this is same latest page as before, this will make all changes
        // visible.
        FeedPageMetadataRepository.PageMetadata newLatestPage;
        // The previous latest page, if it's different. In that case, this is the final step that will make everything visible.
        Optional<FeedPageMetadataRepository.PageMetadata> previousLatestPage;
    }

    @Value
    private static class PackResult {
        FeedPageMetadataRepository.PageMetadata page;
        List<FeedEntityRepository.PageAssignment> assignedEntities;
        List<FeedEntityRepository.PageAssignment> remaining;
    }

    Optional<AssignPagesResult> assignPages(
        Optional<FeedPageMetadataRepository.PageMetadata> maybeLatestPage,
        List<FeedEntityRepository.PageAssignment> unassignedEntities
    ) {
        // impl note: if no new pages are created (old latest = new latest) then previousLatestPage must be empty
        // impl note: generation is old latest page generation + 1, and we set that on all pages we return incl the old latest page

        // NB: I think the tests are complete and pass, but the implementation is ugly

        if (unassignedEntities.isEmpty()) {
            return Optional.empty();
        }

        final int generation = maybeLatestPage.map(x -> x.generation() + 1).orElse(Generations.INITIAL_GENERATION);

        final var assignedEntities = new ArrayList<FeedEntityRepository.PageAssignment>();
        final var newPages = new ArrayList<FeedPageMetadataRepository.PageMetadata>();
        var previousLatestPage = Optional.<FeedPageMetadataRepository.PageMetadata>empty();

        var page = maybeLatestPage.map(p -> new MutablePage(
                    p.pageId(),
                    p.lastModified(),
                    p.prev(),
                    p.contentLength(),
                    p.numberOfEntities()
                )
            )
            .orElseGet(() -> MutablePage.emptyPage(pageIdProvider.newPageId(), Optional.empty()));

        for (var entity : unassignedEntities) {
            if (page.numberOfEntities() >= maxEntitiesPerPage || (page.contentLength() > 0 && page.contentLength() + entity.contentLength() > maxBytesPerPage)) {
                var next = pageIdProvider.newPageId();
                var finalPage = page.finishPage(Optional.of(next), generation);
                if (maybeLatestPage.isPresent() && maybeLatestPage.get().pageId().equals(finalPage.pageId())) {
                    previousLatestPage = Optional.of(finalPage);
                } else {
                    newPages.add(finalPage);
                }

                page = MutablePage.emptyPage(next, Optional.of(finalPage.pageId()));
            }

            page.lastModified(entity.lastModified());
            page.contentLength(page.contentLength() + entity.contentLength());
            page.numberOfEntities(page.numberOfEntities() + 1);
            final var assignedEntity = new FeedEntityRepository.PageAssignment(
                entity.contentId(),
                entity.lastModified(),
                entity.originalLastModified(),
                entity.contentLength(),
                Optional.of(page.pageId())
            );
            assignedEntities.add(assignedEntity);
        }

        return Optional.of(new AssignPagesResult(
            assignedEntities,
            newPages,
            page.finishPage(Optional.empty(), generation),
            previousLatestPage
        ));

        /*
        var firstNext = pageIdProvider.newPageId();
        var maybePack = maybeLatestPage.map(p -> packEntities(p, generation, firstNext, unassignedEntities));

        var packs = new ArrayList<PackResult>();

        var remaining = maybePack.map(x -> x.remaining).orElse(unassignedEntities);
        var prev = maybePack.map(x -> x.page.pageId());
        var next = firstNext;

        while (!remaining.isEmpty()) {
            var newNextId = pageIdProvider.newPageId();
            var basePage = new FeedPageMetadataRepository.PageMetadata(
                next,
                Timestamp.of(Instant.EPOCH),
                prev,
                Optional.empty(),
                0,
                0,
                generation
            );
            var result = packEntities(basePage, generation, newNextId, remaining);
            next = newNextId;
            prev = Optional.of(result.page.pageId());
            remaining = result.remaining;
            packs.add(result);
        }


        var assignedEntities = new ArrayList<FeedEntityRepository.PageAssignment>();
        maybePack.ifPresent(x -> assignedEntities.addAll(x.assignedEntities));
        packs.forEach(x -> assignedEntities.addAll(x.assignedEntities));

        FeedPageMetadataRepository.PageMetadata latestPage;
        if (!packs.isEmpty()) {
            latestPage = packs.remove(packs.size() - 1).page;
        } else if (maybePack.isPresent()) {
            latestPage = maybePack.get().page;
            maybePack = Optional.empty();
        } else {
            throw new IllegalStateException("can't happen, I think");
        }

        var newPages = packs.stream().map(x -> x.page).collect(Collectors.toList());

        return Optional.of(new AssignPagesResult(
            assignedEntities,
            newPages,
            latestPage,
            maybePack.map(x -> x.page)
        ));
*/

        /*final var newPages = new ArrayList<FeedPageMetadataRepository.PageMetadata>();
        final var assignedEntities = new ArrayList<FeedEntityRepository.PageAssignment>();
        var prevPageId = maybeLatestPage.flatMap(FeedPageMetadataRepository.PageMetadata::prev);
        var lastModified = maybeLatestPage.map(x -> x.lastModified()).orElse(Timestamp.of(Instant.EPOCH));
        var currentPageId = maybeLatestPage.map(x -> x.pageId()).orElseGet(pageIdProvider::newPageId);
        int numberOfEntities = maybeLatestPage.map(x -> x.numberOfEntities()).orElse(0);
        long contentLength = maybeLatestPage.map(x -> x.contentLength()).orElse(0L);

        for (var entity : entities) {
            if (numberOfEntities >= maxEntitiesPerPage || (contentLength > 0 && contentLength + entity.contentLength() > maxBytesPerPage)) {
                var nextPageId = pageIdProvider.newPageId();
                var newPage = new FeedPageMetadataRepository.PageMetadata(
                    currentPageId,
                    lastModified,
                    prevPageId,
                    Optional.of(nextPageId),
                    contentLength,
                    numberOfEntities,
                    generation
                );
                newPages.add(newPage);
                prevPageId = Optional.of(currentPageId);
                currentPageId = nextPageId;
                numberOfEntities = 0;
                contentLength = 0;
            }

            lastModified = entity.lastModified();
            numberOfEntities += 1;
            contentLength += entity.contentLength();
            final var assignedEntity = new FeedEntityRepository.PageAssignment(
                entity.contentId(),
                entity.lastModified(),
                entity.originalLastModified(),
                entity.contentLength(),
                Optional.of(currentPageId)
            );
            assignedEntities.add(assignedEntity);
        }

        var latestPage = new FeedPageMetadataRepository.PageMetadata(
            currentPageId,
            lastModified,
            prevPageId,
            Optional.empty(),
            contentLength,
            numberOfEntities,
            generation
        );

        Optional<FeedPageMetadataRepository.PageMetadata> previousLatestPage = Optional.empty();
        if (maybeLatestPage.isPresent() && !newPages.isEmpty()) {
            previousLatestPage = Optional.of(newPages.remove(0));
        }

        return Optional.of(new AssignPagesResult(
            assignedEntities,
            newPages,
            latestPage,
            previousLatestPage
        ));*/

        //throw new UnsupportedOperationException("not yet implemented");
    }

    private PackResult packEntities(
        FeedPageMetadataRepository.PageMetadata page,
        int generation,
        PageId nextPage,
        List<FeedEntityRepository.PageAssignment> unassignedEntities
    ) {
        var assignedEntities = new ArrayList<FeedEntityRepository.PageAssignment>();
        var lastModified = page.lastModified();
        var numberOfEntities = page.numberOfEntities();
        var contentLength = page.contentLength();
        var idx = 0;
        var setNextLink = false;

        for (; idx < unassignedEntities.size(); idx++) {
            var entity = unassignedEntities.get(idx);
            if (numberOfEntities >= maxEntitiesPerPage || (contentLength > 0 && contentLength + entity.contentLength() > maxBytesPerPage)) {
                setNextLink = true;
                break;
            }
            lastModified = entity.lastModified();
            numberOfEntities += 1;
            contentLength += entity.contentLength();
            final var assignedEntity = new FeedEntityRepository.PageAssignment(
                entity.contentId(),
                entity.lastModified(),
                entity.originalLastModified(),
                entity.contentLength(),
                Optional.of(page.pageId())
            );
            assignedEntities.add(assignedEntity);
        }

        var newPage = new FeedPageMetadataRepository.PageMetadata(
            page.pageId(),
            lastModified,
            page.prev(),
            setNextLink ? Optional.of(nextPage) : Optional.empty(),
            contentLength,
            numberOfEntities,
            generation
        );
        return new PackResult(
            newPage,
            assignedEntities,
            unassignedEntities.subList(idx, unassignedEntities.size())
        );
    }
}
