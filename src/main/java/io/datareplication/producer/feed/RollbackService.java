package io.datareplication.producer.feed;

import io.datareplication.model.PageId;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.Value;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@AllArgsConstructor(access = AccessLevel.PACKAGE)
class RollbackService {
    private final FeedEntityRepository feedEntityRepository;
    private final FeedPageMetadataRepository feedPageMetadataRepository;

    @Value
    private static class RollbackActions {
        List<FeedEntityRepository.PageAssignment> entities;
        List<PageId> deletePages;
    }

    /**
     * Perform necessary cleanup actions to roll back a dirty journal state. This service does all its actions as
     * side effects to keep the rollback handling out of the main code path as far as possible. This means once the returned
     * Mono resolves, the repository state has been cleaned and the journal state can be deleted.
     * @param journalState journal info for cleanup
     */
    Mono<Void> rollback(FeedProducerJournalRepository.JournalState journalState) {
        return FeedPageMetadataRepositoryActions
            .getLatest(feedPageMetadataRepository)
            .flatMapMany(maybeLatestPage -> determineRollbackActions(maybeLatestPage, journalState))
            .reduce(new RollbackActions(new ArrayList<>(), new ArrayList<>()), this::accumulateActions)
            .flatMap(actions -> Mono
                .fromCompletionStage(() -> feedEntityRepository.savePageAssignments(actions.entities))
                .then(Mono.fromCompletionStage(() -> feedPageMetadataRepository.delete(actions.deletePages))));
    }

    private RollbackActions accumulateActions(RollbackActions acc, RollbackActions actions) {
        acc.entities.addAll(actions.entities);
        acc.deletePages.addAll(actions.deletePages);
        return acc;
    }

    private Flux<RollbackActions> determineRollbackActions(
        Optional<FeedPageMetadataRepository.@NonNull PageMetadata> maybeLatestPage,
        FeedProducerJournalRepository.JournalState journalState
    ) {
        // Unset the assigned page for all entities on the latest page that are not accounted for in the page header.
        // The currently-visible latest page is the only page that can be "partially" visible, i.e. have entities
        // that have a page assignment but aren't visible yet (and are filtered when getting the page). The page itself
        // is visible, so we can't delete it completely, but new entities were added that need to be unassigned so
        // they can be reassigned. We do this always if we have a latest page because it's a no-op if the page is
        // already clean.
        final var latestPageReset = maybeLatestPage
            .map(this::unassignUnpublishedEntities)
            .orElse(Mono.empty());

        // If any new pages were created, either the switch happened and they are all visible, or the switch didn't
        // happen and none of them are visible. Because of this, we delete all newly-created pages in the journal
        // (= delete the page entries and unassign all entities) when:
        // * there's currently no latest page (in which case no pages are visible anyway)
        // * the current latest page is != the new latest page marked in the journal (new pages were created, but the
        //   switch didn't happen)
        // Otherwise (= the current latest page is the same as the latest page in the journal) we don't delete any
        // new pages because they're already visible. This means either the switchover happened or we didn't create any
        // new pages and only added entities to the old latest page (and that case we already handled above).
        Flux<RollbackActions> pageDeletes = maybeLatestPage.stream().noneMatch(p -> p.pageId().equals(journalState.newLatestPage()))
            ? deletePages(journalState)
            : Flux.empty();

        return pageDeletes.concatWith(latestPageReset);
    }

    private Mono<RollbackActions> unassignUnpublishedEntities(FeedPageMetadataRepository.PageMetadata page) {
        return Mono
            .fromCompletionStage(() -> feedEntityRepository.getPageAssignments(page.pageId()))
            .map(entities -> {
                final var entitiesToUnassign = entities
                    .stream()
                    .skip(page.numberOfEntities())
                    .map(this::resetPageAssignment)
                    .collect(Collectors.toList());
                return new RollbackActions(entitiesToUnassign, List.of());
            });
    }

    private Flux<RollbackActions> deletePages(FeedProducerJournalRepository.JournalState journalState) {
        return Flux
            .fromIterable(journalState.newPages())
            .concatWithValues(journalState.newLatestPage())
            .flatMap(this::deletePage);
    }

    private Mono<RollbackActions> deletePage(PageId page) {
        // To delete a page, we unassign all its entities and delete the page entry.
        return Mono
            .fromCompletionStage(() -> feedEntityRepository.getPageAssignments(page))
            .map(entities -> {
                final var entitiesToUnassign = entities
                    .stream()
                    .map(this::resetPageAssignment)
                    .collect(Collectors.toList());
                return new RollbackActions(entitiesToUnassign, List.of(page));
            });
    }

    private FeedEntityRepository.PageAssignment resetPageAssignment(FeedEntityRepository.PageAssignment pageAssignment) {
        // To reset a page assignment, we:
        // * empty out the pageId field
        // * set lastModified to originalLastModified to revert any timestamp update that may have happened
        // * unset originalLastModified because of ^
        return new FeedEntityRepository.PageAssignment(
            pageAssignment.contentId(),
            pageAssignment.originalLastModified().orElse(pageAssignment.lastModified()),
            Optional.empty(),
            pageAssignment.contentLength(),
            Optional.empty()
        );
    }
}
