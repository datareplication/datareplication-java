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
    private final int networkConcurrency;

    SnapshotConsumerImpl(final HttpClient httpClient,
                         final PageLoader pageLoader,
                         final int networkConcurrency) {
        this.httpClient = httpClient;
        this.pageLoader = pageLoader;
        this.networkConcurrency = networkConcurrency;
    }

    @Override
    public @NonNull CompletionStage<@NonNull SnapshotIndex> loadSnapshotIndex(@NonNull final Url url) {
        return httpClient
            .get(url, HttpResponse.BodyHandlers.ofByteArray())
            .map(response -> Body.fromBytes(response.body()))
            // TODO: wrap exception?
            .map(SnapshotIndex::fromJson)
            .toCompletionStage();
    }

    @Override
    public @NonNull Flow.Publisher<
        @NonNull StreamingPage<@NonNull SnapshotPageHeader, @NonNull SnapshotEntityHeader>
        > streamPages(@NonNull final SnapshotIndex snapshotIndex) {
        return FlowAdapters.toFlowPublisher(streamPagesInternal(snapshotIndex, networkConcurrency));
    }

    @Override
    public @NonNull Flow.Publisher<
        @NonNull Entity<@NonNull SnapshotEntityHeader>
        > streamEntities(@NonNull final SnapshotIndex snapshotIndex) {
        final var flowable = streamPagesInternal(snapshotIndex, networkConcurrency)
            .map(page -> FlowAdapters.toPublisher(page.toCompleteEntities()))
            .flatMap(Flowable::fromPublisher, networkConcurrency);
        return FlowAdapters.toFlowPublisher(flowable);
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
