package io.datareplication.consumer.snapshot;

import io.datareplication.consumer.StreamingPage;
import io.datareplication.internal.http.HttpClient;
import io.datareplication.internal.page.PageLoader;
import io.datareplication.internal.page.WrappedStreamingPage;
import io.datareplication.model.Body;
import io.datareplication.model.Entity;
import io.datareplication.model.HttpHeaders;
import io.datareplication.model.Url;
import io.datareplication.model.snapshot.SnapshotEntityHeader;
import io.datareplication.model.snapshot.SnapshotIndex;
import io.datareplication.model.snapshot.SnapshotPageHeader;
import io.reactivex.rxjava3.core.Flowable;
import lombok.NonNull;
import org.reactivestreams.FlowAdapters;

import java.net.http.HttpResponse;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;

class SnapshotConsumerImpl implements SnapshotConsumer {
    private final HttpClient httpClient;
    private final PageLoader pageLoader;

    // TODO: make configurable in some form
    private static final int NETWORK_CONCURRENCY = 4;

    SnapshotConsumerImpl(final HttpClient httpClient, final PageLoader pageLoader) {
        this.httpClient = httpClient;
        this.pageLoader = pageLoader;
    }

    @Override
    public @NonNull CompletionStage<@NonNull SnapshotIndex> loadSnapshotIndex(@NonNull final Url url) {
        return httpClient
            .get(url, HttpResponse.BodyHandlers.ofByteArray())
            .map(response -> Body.fromBytes(response.body()))
            .map(SnapshotIndex::fromJson)
            .toCompletionStage();
    }

    @Override
    public @NonNull Flow.Publisher<
        @NonNull StreamingPage<@NonNull SnapshotPageHeader, @NonNull SnapshotEntityHeader>
        > streamPages(@NonNull final SnapshotIndex snapshotIndex) {
        return FlowAdapters.toFlowPublisher(streamPagesInternal(snapshotIndex, NETWORK_CONCURRENCY));
    }

    @Override
    public @NonNull Flow.Publisher<
        @NonNull Entity<@NonNull SnapshotEntityHeader>
        > streamEntities(@NonNull final SnapshotIndex snapshotIndex) {
        // TODO: implement
        /*final Flowable<Entity<SnapshotEntityHeader>> flowable = streamPagesInternal(snapshotIndex, NETWORK_CONCURRENCY)
            .map(page -> FlowAdapters.toPublisher(page.toCompleteEntities()))
            .flatMap(Flowable::fromPublisher, NETWORK_CONCURRENCY);
        return FlowAdapters.toFlowPublisher(flowable);*/
        throw new RuntimeException("not implemented");
    }

    private Flowable<
        StreamingPage<SnapshotPageHeader, SnapshotEntityHeader>
        > streamPagesInternal(final SnapshotIndex snapshotIndex, int networkConcurrency) {
        return Flowable
            .fromIterable(snapshotIndex.pages())
            .flatMapSingle(pageLoader::load, false, networkConcurrency)
            .map(this::wrap);
    }

    private StreamingPage<SnapshotPageHeader, SnapshotEntityHeader> wrap(StreamingPage<HttpHeaders, HttpHeaders> page) {
        return new WrappedStreamingPage<>(page,
                                          new SnapshotPageHeader(page.header()),
                                          SnapshotEntityHeader::new);
    }
}
