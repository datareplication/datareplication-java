package io.datareplication.consumer.snapshot;

import com.github.mizosoft.methanol.Methanol;
import io.datareplication.consumer.Authorization;
import io.datareplication.internal.http.HttpClient;
import io.datareplication.internal.page.PageLoader;
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
        // TODO: HTTP timeouts
        private final List<HttpHeader> additionalHeaders;
        private Supplier<Optional<Authorization>> authSupplier;
        private int networkConcurrency;

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

        /**
         * <p>Set the maximum number of pages to download concurrently. Defaults to 2.</p>
         *
         * <p>Setting this to 1 will will perform downloads fully sequentially; this guarantees ordering, i.e. pages
         * will be returned exactly in the order they are listed in the index. Any value >1 may return entities and
         * pages out of order to maximize throughput.</p>
         *
         * @param networkConcurrency the number of pages to download concurrently
         * @throws IllegalArgumentException if the argument is <= 0
         * @return the builder
         */
        public @NonNull Builder networkConcurrency(int networkConcurrency) {
            if (networkConcurrency <= 0) {
                throw new IllegalArgumentException("networkConcurrency must be >= 1");
            }
            this.networkConcurrency = networkConcurrency;
            return this;
        }

        public @NonNull SnapshotConsumer build() {
            final var httpClient = new HttpClient(Methanol.newBuilder()
                                                      .autoAcceptEncoding(true)
                                                      .build());
            final var pageLoader = new PageLoader(httpClient);
            return new SnapshotConsumerImpl(httpClient,
                                            pageLoader,
                                            networkConcurrency);
        }
    }

    static @NonNull Builder builder() {
        return new Builder(new ArrayList<>(),
                           Optional::empty,
                           2);
    }
}
