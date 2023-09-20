package io.datareplication.snapshot.consumer;

import io.datareplication.model.Entity;
import io.datareplication.model.Page;
import io.datareplication.model.Url;
import io.datareplication.model.snapshot.SnapshotEntityHeader;
import io.datareplication.model.snapshot.SnapshotPageHeader;
import io.datareplication.model.snapshot.SnapshotIndex;
import io.datareplication.streaming.StreamingPage;
import lombok.NonNull;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;

public interface SnapshotConsumer {
    // TODO: CompletionStage?
    // TODO: error handling
    @NonNull CompletionStage<@NonNull SnapshotIndex> loadSnapshotIndex(@NonNull Url url);

    // TODO: parallelism setting

    // TODO: error handling
    @NonNull Flow.Publisher<@NonNull StreamingPage<@NonNull SnapshotPageHeader, @NonNull SnapshotEntityHeader>> streamPages(@NonNull SnapshotIndex snapshotIndex);

    @NonNull Flow.Publisher<@NonNull Entity<@NonNull SnapshotEntityHeader>> streamEntities(@NonNull SnapshotIndex snapshotIndex);
}
