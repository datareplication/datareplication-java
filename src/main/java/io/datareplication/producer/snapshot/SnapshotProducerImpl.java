package io.datareplication.producer.snapshot;

import io.datareplication.model.Entity;
import io.datareplication.model.PageId;
import io.datareplication.model.Timestamp;
import io.datareplication.model.Url;
import io.datareplication.model.snapshot.SnapshotEntityHeader;
import io.datareplication.model.snapshot.SnapshotId;
import io.datareplication.model.snapshot.SnapshotIndex;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.FlowableConverter;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.core.SingleSource;
import io.reactivex.rxjava3.schedulers.Schedulers;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import org.reactivestreams.FlowAdapters;
import org.reactivestreams.Subscriber;

import java.time.Clock;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;
import java.util.concurrent.Future;

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


        CompletionStage<CompletionStage<SnapshotIndex>> completionStage = Flowable
            .fromPublisher(FlowAdapters.toPublisher(entities))
            // TODO: Replace Buffer with correct perPage grouping
            .buffer(2)
            .map(x -> {
                PageId pageId = pageIdProvider.newPageId();
                return snapshotPageUrlBuilder.pageUrl(id, pageId);
                // TODO: Save Page to repo
            })
            .reduce(new ArrayList<Url>(), (urls, url) -> {
                urls.add(url);
                return urls;
            })
            .map(pages -> new SnapshotIndex(id, createdAt, pages))
            .flatMap(snapshotIndex -> snapshotIndexRepository.save(snapshotIndex).thenApply(unused -> snapshotIndex))
            .toCompletionStage();

        return completionStage;
        //return snapshotIndexRepository.save(snapshotIndex).thenApply(unused -> snapshotIndex);
    }
}
