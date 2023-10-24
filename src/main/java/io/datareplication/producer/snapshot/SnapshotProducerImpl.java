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
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import org.reactivestreams.FlowAdapters;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;

// TODO: impl the impl
@AllArgsConstructor
class SnapshotProducerImpl implements SnapshotProducer {
    private final SnapshotPageUrlBuilder snapshotPageUrlBuilder;
    private final SnapshotIndexRepository snapshotIndexRepository;
    private final SnapshotPageRepository snapshotPageRepository;
    private final PageIdProvider pageIdProvider;
    private final SnapshotIdProvider snapshotIdProvider;
    private final long maxBytesPerPage;
    private final Clock clock;

    @Override
    public @NonNull CompletionStage<@NonNull SnapshotIndex> produce(
        final @NonNull Flow.Publisher<@NonNull Entity<@NonNull SnapshotEntityHeader>> entities
    ) {
        SnapshotId id = snapshotIdProvider.newSnapshotId();
        Timestamp createdAt = Timestamp.of(clock.instant());


        CompletionStage<SnapshotIndex> completionStage = Flowable
            .fromPublisher(FlowAdapters.toPublisher(entities))
            // TODO: Replace Buffer with correct perPage grouping
            .buffer(2)
            .flatMap(x -> {
                PageId pageId = pageIdProvider.newPageId();
                Url pageUrl = snapshotPageUrlBuilder.pageUrl(id, pageId);
                return Flowable.fromCompletionStage(snapshotPageRepository.save(id, pageId, pageOf(x)).thenApply(unused -> pageUrl));
            })
            .reduce(new ArrayList<Url>(), (urls, url) -> {
                urls.add(url);
                return urls;
            })
            .map(pages -> new SnapshotIndex(id, createdAt, pages))
            .flatMap(snapshotIndex -> Single.fromCompletionStage(snapshotIndexRepository.save(snapshotIndex).thenApply(
                unused -> snapshotIndex
            )))
            .toCompletionStage();

        return completionStage;
    }

    private Page<SnapshotPageHeader, SnapshotEntityHeader> pageOf(final List<Entity<SnapshotEntityHeader>> entities) {
        return new Page<>(new SnapshotPageHeader(HttpHeaders.EMPTY), entities);
    }
}
