package io.datareplication.producer.snapshot;

import io.datareplication.model.Entity;
import io.datareplication.model.HttpHeaders;
import io.datareplication.model.Page;
import io.datareplication.model.PageId;
import io.datareplication.model.Timestamp;
import io.datareplication.model.Url;
import io.datareplication.model.snapshot.SnapshotEntityHeader;
import io.datareplication.model.snapshot.SnapshotId;
import io.datareplication.model.snapshot.SnapshotIndex;
import io.datareplication.model.snapshot.SnapshotPageHeader;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import reactor.adapter.JdkFlowAdapter;
import reactor.core.publisher.Mono;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicLong;

@AllArgsConstructor(access = AccessLevel.PACKAGE)
class SnapshotProducerImpl implements SnapshotProducer {
    private final SnapshotPageUrlBuilder snapshotPageUrlBuilder;
    private final SnapshotIndexRepository snapshotIndexRepository;
    private final SnapshotPageRepository snapshotPageRepository;
    private final PageIdProvider pageIdProvider;
    private final SnapshotIdProvider snapshotIdProvider;
    private final long maxBytesPerPage;
    private final long maxEntitiesPerPage;
    private final Clock clock;

    @Override
    public @NonNull CompletionStage<@NonNull SnapshotIndex> produce(
        final @NonNull Flow.Publisher<@NonNull Entity<@NonNull SnapshotEntityHeader>> entities
    ) {
        AtomicLong currentBytesForPage = new AtomicLong(0L);
        AtomicLong currentEntitiesForPage = new AtomicLong(0L);
        SnapshotId id = snapshotIdProvider.newSnapshotId();
        Timestamp createdAt = Timestamp.of(clock.instant());

        return JdkFlowAdapter.flowPublisherToFlux(entities)
            .bufferUntil(entity -> {
                long bytes = entity.body().contentLength();
                if (currentBytesForPage.addAndGet(bytes) > maxBytesPerPage
                    || currentEntitiesForPage.incrementAndGet() > maxEntitiesPerPage) {
                    currentBytesForPage.set(bytes);
                    currentEntitiesForPage.set(1L);
                    return true;
                } else {
                    return false;
                }
            }, true)
            /*
                TODO:
                    Research: flatMapSequential retains the order of the elements, but does it come with a cost?
                    Is it important to us to retain the order of the elements?
             */
            .flatMapSequential(entityList -> {
                PageId pageId = pageIdProvider.newPageId();
                Url pageUrl = snapshotPageUrlBuilder.pageUrl(id, pageId);
                return Mono
                    .fromCompletionStage(snapshotPageRepository.save(id, pageId, pageOf(pageId, entityList)))
                    .then(Mono.fromCallable(() -> pageUrl));
            })
            .reduce(new ArrayList<Url>(), (urls, url) -> {
                urls.add(url);
                return urls;
            })
            .map(pages -> new SnapshotIndex(id, createdAt, pages))
            .flatMap(snapshotIndex -> Mono.fromCompletionStage(snapshotIndexRepository.save(snapshotIndex))
                .then(Mono.fromCallable(() -> snapshotIndex))
            )
            .toFuture();
    }

    private Page<SnapshotPageHeader, SnapshotEntityHeader> pageOf(PageId pageId,
                                                                  final List<Entity<SnapshotEntityHeader>> entities) {
        return new Page<>(new SnapshotPageHeader(HttpHeaders.EMPTY), pageId.boundary(), entities);
    }
}
