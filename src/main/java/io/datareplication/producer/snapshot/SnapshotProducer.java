package io.datareplication.producer.snapshot;

import io.datareplication.model.Entity;
import io.datareplication.model.snapshot.SnapshotEntityHeader;
import io.datareplication.model.snapshot.SnapshotIndex;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;

public interface SnapshotProducer {
    @NonNull CompletionStage<@NonNull SnapshotIndex> produce(
        @NonNull Flow.Publisher<@NonNull Entity<@NonNull SnapshotEntityHeader>> entities);

    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class Builder {

        public @NonNull SnapshotProducer build() {
            throw new RuntimeException("not implemented");
        }
    }

    static @NonNull SnapshotProducer.Builder builder() {
        return new SnapshotProducer.Builder();
    }
}
