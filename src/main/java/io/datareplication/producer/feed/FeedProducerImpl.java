package io.datareplication.producer.feed;

import io.datareplication.model.Body;
import io.datareplication.model.Entity;
import io.datareplication.model.Timestamp;
import io.datareplication.model.feed.FeedEntityHeader;
import io.datareplication.model.feed.OperationType;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import reactor.core.publisher.Mono;

import java.time.Clock;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

@AllArgsConstructor(access = AccessLevel.PACKAGE)
class FeedProducerImpl implements FeedProducer {
    private final FeedEntityRepository feedEntityRepository;
    private final FeedPageMetadataRepository feedPageMetadataRepository;
    private final FeedProducerJournalRepository feedProducerJournalRepository;
    private final Clock clock;
    private final RandomContentIdProvider contentIdProvider;
    private final RollbackService rollbackService;
    private final NewEntityTimestampsService newEntityTimestampsService;
    private final AssignPagesService assignPagesService;
    private final int assignPagesLimit;

    @Override
    public @NonNull CompletionStage<Void> publish(@NonNull final OperationType operationType,
                                                  @NonNull final Body body) {
        return publish(operationType, body, Optional.empty());
    }

    @Override
    public @NonNull CompletionStage<Void> publish(@NonNull final OperationType operationType,
                                                  @NonNull final Body body,
                                                  @NonNull final Object userData) {
        return publish(operationType, body, Optional.of(userData));
    }

    private CompletionStage<Void> publish(OperationType operationType, Body body, Optional<Object> userData) {
        final var header = new FeedEntityHeader(Timestamp.of(clock.instant()),
            operationType,
            contentIdProvider.newContentId());
        final var entity = new Entity<>(header, body, userData);
        return publish(entity);
    }

    @Override
    public @NonNull CompletionStage<Void> publish(@NonNull final Entity<@NonNull FeedEntityHeader> entity) {
        return feedEntityRepository.append(entity);
    }

    // TODO: Let's talk about assignPagesLimit. Should this be a limit per call (i.e. every time you call assignPages, it
    //  will process at most limit entities, i.e. it will never return a number > the limit? Or is it a batch size, i.e.
    //  assignPages will always process as much as it can find, but it will split the work into blocks of at most limit
    //  entities to prevent OOM? In this case the return value might well be > limit. But it might also cause assignPages
    //  to effectively loop for an indeterminate amount of time if there's a constant drip of entities.
    //  I'm leaning towards hard limit, because you can easily build the loop on top, but you can't easily get
    //  guaranteed-limited work units from the loop version.
    @Override
    public @NonNull CompletionStage<Integer> assignPages() {
        return Mono
            // Step 1: load journal state to check if we need to recover from a broken state.
            .fromCompletionStage(feedProducerJournalRepository::get)
            .flatMap(maybeJournalState -> maybeJournalState
                .map(journalState -> rollbackService
                    // If we have a broken state, we let RollbackService handle the finer details.
                    .rollback(journalState)
                    // Once the rollback is done, we clean up the journal entry because the state is now clean.
                    .then(Mono.fromCompletionStage(feedProducerJournalRepository::delete)))
                .orElse(Mono.empty()))
            // Step 2: load the current latest page so we can extend it/link it up.
            .then(Mono.fromCompletionStage(feedPageMetadataRepository::getLatest))
            // Also load all entities that currently aren't assigned to a page (up to a max).
            .zipWith(Mono.fromCompletionStage(() -> feedEntityRepository.getUnassigned(assignPagesLimit)))
            // Step 3: calculate page assignments to make, pages to create, and existing pages to update.
            .map((args) -> {
                final var maybeLatest = args.getT1();
                final var unassigned = args.getT2();

                // TODO: lag/delay? Filter out everything not old enough first or after postdating?
                // Make sure all timestamps are not before the latest page, keeping ordering as much as possible.
                final var unassignedWithUpdatedTimestamps = maybeLatest
                    .map(latest -> newEntityTimestampsService.updatedEntityTimestamps(latest, unassigned))
                    .orElse(unassigned);

                // Build new pages etc. and return the steps to take.
                return assignPagesService.assignPages(maybeLatest, unassignedWithUpdatedTimestamps);
            })
            // Step 4: apply all the changes we determined previously.
            .flatMap(maybeAssignPagesResult -> maybeAssignPagesResult
                .map(this::saveAssignPagesResult)
                .orElse(Mono.just(0))
            )
            .toFuture();
    }

    private Mono<Integer> saveAssignPagesResult(AssignPagesService.AssignPagesResult assignPagesResult) {
        final var journalState = new FeedProducerJournalRepository.JournalState(
            assignPagesResult
                .newPages()
                .stream()
                .map(FeedPageMetadataRepository.PageMetadata::pageId)
                .collect(Collectors.toList()),
            assignPagesResult.newLatestPage().pageId(),
            assignPagesResult
                .previousLatestPage()
                .map(FeedPageMetadataRepository.PageMetadata::pageId)
        );

        return Mono
            // Step 4.1: save journal state so we can roll back if we crash
            .fromCompletionStage(() -> feedProducerJournalRepository.save(journalState))
            // Step 4.2: save all entities
            // we can just do this because FeedPageProvider checks the page metadata and filters out any entities that
            // aren't acknowledged in the page metadata so none of this is visible for now
            .then(Mono.fromCompletionStage(() -> feedEntityRepository.savePageAssignments(assignPagesResult.entityPageAssignments())))
            // Step 4.3: save page with rotated generation
            // If we need to rotate the generation on the still-current latest page, we need to do it before we save the
            // new and updated latest page objects to uphold the ordering rules for generations.
            .then(Mono.fromCompletionStage(() -> feedPageMetadataRepository.save(listFromOptional(assignPagesResult.newGeneration()))))
            // Step 4.4: create new pages
            // It's now possible to access these pages via their ID, but they're not yet reachable from the (current) latest
            // page. That's why we don't need to care about order yet.
            .then(Mono.fromCompletionStage(() -> feedPageMetadataRepository.save(assignPagesResult.newPages())))
            // Step 4.5: save the new latest page
            // If the feed is currently empty, saving this page will immediately make it the new latest page and make
            // it and every other new page visible, which is why we're waiting until all other new pages are saved
            // before we do this one.
            // If this is the same as the previous latest page (i.e. no new pages were created), this will also make
            // its contents immediately visible.
            // If there already is a latest page (i.e. the feed wasn't empty before this iteration) this page won't
            // be visible yet. While both of them won't have a next link and thus qualify as latest, this one will
            // have a higher generation number so the old one will be preferred when loading.
            .then(Mono.fromCompletionStage(() -> feedPageMetadataRepository.save(List.of(assignPagesResult.newLatestPage()))))
            // Step 4.6: update the old latest page
            // This will make everything visible, so we do this last. By setting its next link, this page doesn't
            // qualify as the latest page any more and the new one becomes the latest page.
            .then(Mono.fromCompletionStage(() -> feedPageMetadataRepository.save(listFromOptional(assignPagesResult.previousLatestPage()))))
            // Step 4.7: if we successfully reached this point, the new repo state is clean and we can remove our journal entry.
            .then(Mono.fromCompletionStage(feedProducerJournalRepository::delete))
            .thenReturn(assignPagesResult.entityPageAssignments().size());
    }

    private <T> List<T> listFromOptional(Optional<T> optional) {
        return optional
            .map(Collections::singletonList)
            .orElse(Collections.emptyList());
    }
}
