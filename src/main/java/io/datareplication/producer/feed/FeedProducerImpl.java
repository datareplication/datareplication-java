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
        // TODO: check journal, rollback if necessary
        // TODO: journal probably should save differently (old latest page separate from the others) for easier rollback
        //  because there's only two possibilities:
        //  - latest page switched -> everything clean because things are visible
        //  - old latest page -> switch didn't happen, delete all new pages and clean up non-visible entities for old (still current) latest page
        //                       or maybe: there were no new pages and the page entry was or was not updated, but we still just clean up
        //                       all non-visible entities because if the page entry was updated they'd be visible
        //  clean up entity = unset page ID, copy originalLastModified to lastModified if set and unset originalLastModified

        return Mono
            .fromCompletionStage(feedPageMetadataRepository::getLatest)
            .zipWith(Mono.fromCompletionStage(() -> feedEntityRepository.getUnassigned(assignPagesLimit)))
            .map((args) -> {
                final var maybeLatest = args.getT1();
                final var unassigned = args.getT2();

                // TODO: lag/delay? Filter out everything not old enough first or after postdating?
                // TODO: actually maybe we have to make the timestamping rollbackable to avoid reorderings if page assignments
                //  are rolled back but the timestamp changes remain?
                //  Plan: if we update lastModified, save the original value in originalLastModified. Then rollback can just
                //  undo that together with unsetting the page id
                // step 1: make sure all timestamps are not before the latest page, keeping ordering as much as possible
                final var unassignedWithUpdatedTimestamps = maybeLatest
                    .map(latest -> newEntityTimestampsService.updatedEntityTimestamps(latest, unassigned))
                    .orElse(unassigned);

                // step 2: given the old latest page, generate updated PageAssignments and new and updated PageMetadata
                return assignPagesService.assignPages(maybeLatest, unassignedWithUpdatedTimestamps);
            })
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
            // step 1: save journal state
            .fromCompletionStage(() -> feedProducerJournalRepository.save(journalState))
            // step 2: save all entities
            // we can just do this because FeedPageProvider checks the page metadata and filters out any entities that
            // aren't acknowledged in the page metadata so none of this is visible for now
            .then(Mono.fromCompletionStage(() -> feedEntityRepository.savePageAssignments(assignPagesResult.entityPageAssignments())))
            // step 3: save page with rotated generation
            // If we need to rotate the generation on the still-current latest page, we need to do it before we save the
            // new and updated latest page objects to uphold the ordering rules for generations.
            .then(Mono.fromCompletionStage(() -> feedPageMetadataRepository.save(listFromOptional(assignPagesResult.newGeneration()))))
            // step 4: create new pages
            // It's now possible to access these pages via their ID, but they're not yet reachable from the (current) latest
            // page. That's why we don't need to care about order yet.
            .then(Mono.fromCompletionStage(() -> feedPageMetadataRepository.save(assignPagesResult.newPages())))
            // step 5: save the new latest page
            // If the feed is currently empty, saving this page will immediately make it the new latest page and make
            // it and every other new page visible, which is why we're waiting until all other new pages are saved
            // before we do this one.
            // If this is the same as the previous latest page (i.e. no new pages were created), this will also make
            // its contents immediately visible.
            // If there already is a latest page (i.e. the feed wasn't empty before this iteration) this page won't
            // be visible yet. While both of them won't have a next link and thus qualify as latest, this one will
            // have a higher generation number so the old one will be preferred when loading.
            .then(Mono.fromCompletionStage(() -> feedPageMetadataRepository.save(List.of(assignPagesResult.newLatestPage()))))
            // step 6: update the old latest page
            // This will make everything visible, so we do this last. By setting its next link, this page doesn't
            // qualify as the latest page any more and the new one becomes the latest page.
            .then(Mono.fromCompletionStage(() -> feedPageMetadataRepository.save(listFromOptional(assignPagesResult.previousLatestPage()))))
            // step 7: if we successfully reached this point, the new repo state is clean and we can remove our journal entry.
            .then(Mono.fromCompletionStage(feedProducerJournalRepository::delete))
            .thenReturn(assignPagesResult.entityPageAssignments().size());
    }

    private <T> List<T> listFromOptional(Optional<T> optional) {
        return optional
            .map(Collections::singletonList)
            .orElse(Collections.emptyList());
    }
}
