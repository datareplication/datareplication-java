package io.datareplication.consumer.snapshot;

import io.datareplication.consumer.ConsumerException;
import io.datareplication.consumer.StreamingPage;
import io.datareplication.internal.http.HttpClient;
import io.datareplication.internal.page.PageLoader;
import io.datareplication.internal.page.WrappedStreamingPage;
import io.datareplication.model.Body;
import io.datareplication.model.ContentType;
import io.datareplication.model.Entity;
import io.datareplication.model.HttpHeaders;
import io.datareplication.model.Url;
import io.datareplication.model.snapshot.SnapshotEntityHeader;
import io.datareplication.model.snapshot.SnapshotIndex;
import io.datareplication.model.snapshot.SnapshotPageHeader;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import reactor.adapter.JdkFlowAdapter;
import reactor.core.Exceptions;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;
import java.util.function.Function;

@AllArgsConstructor(access = AccessLevel.PACKAGE)
class SnapshotConsumerImpl implements SnapshotConsumer {
    private static final ContentType APPLICATION_JSON = ContentType.of("application/json");

    private final HttpClient httpClient;
    private final PageLoader pageLoader;
    private final int networkConcurrency;
    private final boolean delayErrors;

    @Override
    public @NonNull CompletionStage<@NonNull SnapshotIndex> loadSnapshotIndex(@NonNull final Url url) {
        return httpClient
            .get(url, HttpResponse.BodyHandlers.ofByteArray())
            // safety: ok because we "own" the byte array and aren't modifying it
            .map(response -> Body.fromBytesUnsafe(response.body(), APPLICATION_JSON))
            .map(json -> {
                try {
                    return SnapshotIndex.fromJson(json);
                } catch (IOException e) {
                    throw new IllegalStateException("unexpected IOException thrown by Body; bug?", e);
                }
            })
            .toFuture();
    }

    @Override
    public @NonNull Flow.Publisher<
        @NonNull StreamingPage<@NonNull SnapshotPageHeader, @NonNull SnapshotEntityHeader>
        > streamPages(@NonNull final SnapshotIndex snapshotIndex) {
        return JdkFlowAdapter.publisherToFlowPublisher(
            streamPagesInternal(snapshotIndex, networkConcurrency)
                .onErrorResume(this::rewrapCompositeErrors)
        );
    }

    @Override
    public @NonNull Flow.Publisher<
        @NonNull Entity<@NonNull SnapshotEntityHeader>
        > streamEntities(@NonNull final SnapshotIndex snapshotIndex) {
        final var entities = streamPagesInternal(snapshotIndex, networkConcurrency)
            .map(page -> JdkFlowAdapter.flowPublisherToFlux(page.toCompleteEntities()));
        // Not sure about prefetch here, maybe this needs to be tuned?
        final var flux = (delayErrors
            ? entities.flatMapDelayError(Function.identity(), networkConcurrency, 1)
            : entities.flatMap(Function.identity(), networkConcurrency, 1)
        )
            .onErrorResume(this::rewrapCompositeErrors);
        return JdkFlowAdapter.publisherToFlowPublisher(flux);
    }

    private Flux<
        StreamingPage<SnapshotPageHeader, SnapshotEntityHeader>
        > streamPagesInternal(final SnapshotIndex snapshotIndex, int networkConcurrency) {
        final var pages = Flux.fromIterable(snapshotIndex.pages());
        // prefetch=1 makes sense here because pageLoader::load returns a publisher with exactly one element so we can't
        // prefetch more than 1 anyway.
        return (delayErrors
            ? pages.flatMapDelayError(pageLoader::load, networkConcurrency, 1)
            : pages.flatMap(pageLoader::load, networkConcurrency, 1))
            .map(this::wrapPage);
    }

    private StreamingPage<SnapshotPageHeader, SnapshotEntityHeader> wrapPage(
        StreamingPage<HttpHeaders, HttpHeaders> page
    ) {
        return new WrappedStreamingPage<>(page,
                                          new SnapshotPageHeader(page.header()),
                                          SnapshotEntityHeader::new);
    }

    private <T> Flux<T> rewrapCompositeErrors(Throwable exception) {
        final var unwrapped = Exceptions.unwrapMultiple(exception);
        if (unwrapped.size() > 1) {
            final var rewrapped = new ConsumerException.CollectedErrors(unwrapped);
            return Flux.error(rewrapped);
        } else {
            return Flux.error(exception);
        }
    }
}
