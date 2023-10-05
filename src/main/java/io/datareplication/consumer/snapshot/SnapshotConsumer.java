package io.datareplication.consumer.snapshot;

import io.datareplication.consumer.Authorization;
import io.datareplication.model.Entity;
import io.datareplication.model.HttpHeader;
import io.datareplication.model.Url;
import io.datareplication.model.snapshot.SnapshotEntityHeader;
import io.datareplication.model.snapshot.SnapshotPageHeader;
import io.datareplication.model.snapshot.SnapshotIndex;
import io.datareplication.consumer.StreamingPage;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;
import java.util.function.Supplier;

public interface SnapshotConsumer {
    // TODO: CompletionStage?
    // TODO: error handling
    @NonNull CompletionStage<@NonNull SnapshotIndex> loadSnapshotIndex(@NonNull Url url);

    // TODO: parallelism setting

    // TODO: error handling
    @NonNull Flow.Publisher<
        @NonNull StreamingPage<@NonNull SnapshotPageHeader, @NonNull SnapshotEntityHeader>
        > streamPages(@NonNull SnapshotIndex snapshotIndex);

    @NonNull Flow.Publisher<
        @NonNull Entity<@NonNull SnapshotEntityHeader>
        > streamEntities(@NonNull SnapshotIndex snapshotIndex);

    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class Builder {
        // TODO: threadpool
        // TODO: parallelism setting?
        private final List<HttpHeader> additionalHeaders;
        private Supplier<Optional<Authorization>> authSupplier;

        public @NonNull Builder additionalHeaders(@NonNull HttpHeader... headers) {
            additionalHeaders.addAll(Arrays.asList(headers));
            return this;
        }

        public @NonNull Builder authorization(@NonNull Authorization authorization) {
            return this.authorization(() -> authorization);
        }

        public @NonNull Builder authorization(@NonNull Supplier<@NonNull Authorization> authorization) {
            authSupplier = () -> Optional.of(authorization.get());
            return this;
        }

        public @NonNull SnapshotConsumer build() {
            throw new RuntimeException("not implemented");
        }
    }

    static @NonNull Builder builder() {
        return new Builder(new ArrayList<>(),
                           Optional::empty);
    }
}
