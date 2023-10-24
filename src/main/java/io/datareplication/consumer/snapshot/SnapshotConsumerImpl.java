package io.datareplication.consumer.snapshot;

import io.datareplication.consumer.StreamingPage;
import io.datareplication.internal.http.HttpClient;
import io.datareplication.internal.page.PageLoader;
import io.datareplication.model.Body;
import io.datareplication.model.Entity;
import io.datareplication.model.Url;
import io.datareplication.model.snapshot.SnapshotEntityHeader;
import io.datareplication.model.snapshot.SnapshotIndex;
import io.datareplication.model.snapshot.SnapshotPageHeader;
import lombok.NonNull;

import java.net.http.HttpResponse;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;

class SnapshotConsumerImpl implements SnapshotConsumer {
    private final HttpClient httpClient;
    private final PageLoader pageLoader;

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
        // TODO
        throw new RuntimeException("not implemented");
    }

    @Override
    public @NonNull Flow.Publisher<
        @NonNull Entity<@NonNull SnapshotEntityHeader>
        > streamEntities(@NonNull final SnapshotIndex snapshotIndex) {
        // TODO
        throw new RuntimeException("not implemented");
    }
}
