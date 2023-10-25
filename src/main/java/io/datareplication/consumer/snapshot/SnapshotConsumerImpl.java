package io.datareplication.consumer.snapshot;

import io.datareplication.consumer.ConsumerException;
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
import io.reactivex.rxjava3.exceptions.CompositeException;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import org.reactivestreams.FlowAdapters;

import java.net.http.HttpResponse;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;

@AllArgsConstructor(access = AccessLevel.PACKAGE)
class SnapshotConsumerImpl implements SnapshotConsumer {
    private final HttpClient httpClient;
    private final PageLoader pageLoader;
    private final int networkConcurrency;
    private final boolean delayErrors;

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
        return FlowAdapters.toFlowPublisher(
            streamPagesInternal(snapshotIndex, networkConcurrency)
                .onErrorResumeNext(this::rewrapCompositeErrors)
        );
    }

    @Override
    public @NonNull Flow.Publisher<
        @NonNull Entity<@NonNull SnapshotEntityHeader>
        > streamEntities(@NonNull final SnapshotIndex snapshotIndex) {
        final var flowable = streamPagesInternal(snapshotIndex, networkConcurrency)
            .map(page -> FlowAdapters.toPublisher(page.toCompleteEntities()))
            .flatMap(Flowable::fromPublisher, delayErrors, networkConcurrency)
            .onErrorResumeNext(this::rewrapCompositeErrors);
        return FlowAdapters.toFlowPublisher(flowable);
    }

    private Flowable<
        StreamingPage<SnapshotPageHeader, SnapshotEntityHeader>
        > streamPagesInternal(final SnapshotIndex snapshotIndex, int networkConcurrency) {
        return Flowable
            .fromIterable(snapshotIndex.pages())
            .flatMapSingle(pageLoader::load, delayErrors, networkConcurrency)
            .map(this::wrapPage);
    }

    private StreamingPage<SnapshotPageHeader, SnapshotEntityHeader> wrapPage(StreamingPage<HttpHeaders, HttpHeaders> page) {
        return new WrappedStreamingPage<>(page,
                                          new SnapshotPageHeader(page.header()),
                                          SnapshotEntityHeader::new);
    }

    private <T> Flowable<T> rewrapCompositeErrors(Throwable exception) {
        if (exception instanceof CompositeException) {
            final var compositeException = (CompositeException) exception;
            final var rewrapped = new ConsumerException.CollectedErrors(compositeException.getExceptions());
            return Flowable.error(rewrapped);
        } else {
            return Flowable.error(exception);
        }
    }
}
