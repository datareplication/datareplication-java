package io.datareplication.producer.feed;

import io.datareplication.model.Body;
import io.datareplication.model.Entity;
import io.datareplication.model.PageId;
import io.datareplication.model.Timestamp;
import io.datareplication.model.feed.FeedEntityHeader;
import io.datareplication.model.feed.OperationType;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import reactor.core.publisher.Mono;

import java.time.Clock;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

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

        return Mono
            .fromCompletionStage(feedPageMetadataRepository::getLatest)
            .flatMap(maybeLatest -> {
                // if present and generation is at some safe limit: reset to a low generation and save again
                // this means we can safely increment the generation in later steps
                // no journaling is needed because this is a single atomic operation that doesn't need rollback
                // TODO: impl
                return Mono.just(maybeLatest);
            })
            .zipWith(Mono.fromCompletionStage(() -> feedEntityRepository.getUnassigned(assignPagesLimit)))
            .map((args) -> {
                final var maybeLatest = args.getT1();
                final var unassigned = args.getT2();

                // TODO: lag/delay? Filter out everything not old enough first or after postdating?
                // step 1: make sure all timestamps are not before the latest page, keeping ordering as much as possible
                final var unassignedWithUpdatedTimestamps = maybeLatest
                    .map(latest -> newEntityTimestampsService.updatedEntityTimestamps(latest, unassigned))
                    .orElse(unassigned);

                // step 2: given the old latest page, generate updated PageAssignments and new and updated PageMetadata
                return assignPagesService.assignPages(maybeLatest, unassignedWithUpdatedTimestamps);
            })
            .flatMap(assignPagesResult -> {
                // step 3: mark every modified page as potentially dirty
                final var inProgressPages = new ArrayList<PageId>();
                for (var newPage : assignPagesResult.newPages()) {
                    inProgressPages.add(newPage.pageId());
                }
                assignPagesResult.previousLatestPage().ifPresent(pageMetadata -> inProgressPages.add(pageMetadata.pageId()));
                assignPagesResult.newLatestPage().ifPresent(pageMetadata -> inProgressPages.add(pageMetadata.pageId()));

                return Mono
                    .fromCompletionStage(() -> feedProducerJournalRepository.saveInProgressPages(inProgressPages))
                    .thenReturn(assignPagesResult);
            })
            .flatMap(assignPagesResult -> {
                // step 4: save all entities
                // we can just do this because FeedPageProvider checks the page metadata and filters out any entities that
                // aren't acknowledged in the page metadata so none of this is visible for now
                return Mono
                    .fromCompletionStage(() -> feedEntityRepository.savePageAssignments(assignPagesResult.entityPageAssignments()))
                    .thenReturn(assignPagesResult);
            })
            .flatMap(assignPagesResult -> {
                // step 5: create new pages
                // It's now possible to access these pages via their ID, but they're not yet reachable from the (current) latest
                // page. That's why we don't need to care about order yet.
                return Mono
                    .fromCompletionStage(() -> feedPageMetadataRepository.save(assignPagesResult.newPages()))
                    .thenReturn(assignPagesResult);
            })
            .flatMap(assignPagesResult -> {
                // step 6: save the new latest page
                // If the feed is currently empty, saving this page will immediately make it the new latest page and make
                // it and every other new page visible, which is why we're waiting until all other new pages are saved
                // before we do this one.
                // If there already is a latest page (i.e. the feed wasn't empty before this iteration) this page won't
                // be visible yet. While both of them won't have a next link and thus qualify as latest, this one will
                // have a higher generation number so the old one will be preferred when loading.
                return Mono
                    .fromCompletionStage(() -> feedPageMetadataRepository.save(listFromOptional(assignPagesResult.newLatestPage())))
                    .thenReturn(assignPagesResult);
            })
            .flatMap(assignPagesResult -> {
                // step 7: update the old latest page
                // This will make everything visible, so we do this last. By setting its next link, this page doesn't
                // qualify as the latest page any more and the new one becomes the latest page.
                return Mono
                    .fromCompletionStage(() -> feedPageMetadataRepository.save(listFromOptional(assignPagesResult.previousLatestPage())))
                    .thenReturn(assignPagesResult);
            })
            .flatMap(assignPagesResult -> {
                // step 8: if we successfully reached this point, the new repo state is clean and we can remove our journal entry.
                return Mono
                    .fromCompletionStage(() -> feedProducerJournalRepository.saveInProgressPages(Collections.emptyList()))
                    .thenReturn(assignPagesResult.entityPageAssignments().size());
            })
            .toFuture();
    }

    private <T> List<T> listFromOptional(Optional<T> optional) {
        return optional
            .map(Collections::singletonList)
            .orElse(Collections.emptyList());
    }
}
