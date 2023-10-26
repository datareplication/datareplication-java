package io.datareplication.consumer.snapshot;

import io.datareplication.consumer.Authorization;
import io.datareplication.internal.http.AuthSupplier;
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

// TODO: docs

public interface SnapshotConsumer {
    @NonNull CompletionStage<@NonNull SnapshotIndex> loadSnapshotIndex(@NonNull Url url);

    @NonNull Flow.Publisher<
        @NonNull StreamingPage<@NonNull SnapshotPageHeader, @NonNull SnapshotEntityHeader>
        > streamPages(@NonNull SnapshotIndex snapshotIndex);

    @NonNull Flow.Publisher<
        @NonNull Entity<@NonNull SnapshotEntityHeader>
        > streamEntities(@NonNull SnapshotIndex snapshotIndex);

    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class Builder {
        private final List<HttpHeader> additionalHeaders;
        private AuthSupplier authSupplier;
        private int networkConcurrency;
        private boolean delayErrors;

        // TODO: HTTP timeouts
        // TODO: impl headers

        public @NonNull Builder additionalHeaders(@NonNull HttpHeader... headers) {
            additionalHeaders.addAll(Arrays.asList(headers));
            return this;
        }

        /**
         * Use the given {@link Authorization} for every HTTP request made by this consumer.
         * @param authorization the Authorization to use for HTTP requests
         * @return this builder
         */
        public @NonNull Builder authorization(@NonNull Authorization authorization) {
            authSupplier = AuthSupplier.constant(authorization);
            return this;
        }

        /**
         * <p>Use the given function to get the {@link Authorization} to use for HTTP requests made by this consumer.
         * The function is called once for each request.</p>
         *
         * <p>The given function is expected to return quickly to avoid holding up requests. Because of that, the
         * function should not perform any network requests. If your service requires a fresh authentication token
         * periodically, the recommended approach is to perform the refresh out-of-band (e.g. on a separate thread
         * with a timer) and write the new token to a shared memory location that the supplier function then reads
         * from.</p>
         *
         * @param authorizationSupplier a function returning the Authorization to use for HTTP requests
         * @return this builder
         */
        public @NonNull Builder authorization(@NonNull Supplier<@NonNull Authorization> authorizationSupplier) {
            authSupplier = AuthSupplier.supplier(authorizationSupplier);
            return this;
        }

        /**
         * <p>Set the maximum number of pages to download concurrently. Defaults to 2.</p>
         *
         * <p>Setting this to 1 will will perform downloads fully sequentially; this guarantees ordering, i.e. pages
         * will be returned exactly in the order they are listed in the index. Any value &gt;1 may return entities and
         * pages out of order to maximize throughput.</p>
         *
         * @param networkConcurrency the number of pages to download concurrently
         * @return the builder
         * @throws IllegalArgumentException if the argument is &lt;= 0
         */
        public @NonNull Builder networkConcurrency(int networkConcurrency) {
            if (networkConcurrency <= 0) {
                throw new IllegalArgumentException("networkConcurrency must be >= 1");
            }
            this.networkConcurrency = networkConcurrency;
            return this;
        }

        /**
         * <p>When enabled, collect all errors and raise them at the end after all other pages/entities have been
         * consumed. Defaults to false, i.e. any error terminates the stream immediately.</p>
         *
         * <p>When this setting is enabled, errors while downloading and parsing pages will be silently collected. Once
         * all pages have been downloaded and returned, the collected exceptions will be raised on the stream wrapped
         * in a {@link io.datareplication.consumer.ConsumerException.CollectedErrors} object.</p>
         *
         * @param delayErrors when true, delay all errors until the entire snapshot has been processed
         * @return the builder
         */
        public @NonNull Builder delayErrors(boolean delayErrors) {
            this.delayErrors = delayErrors;
            return this;
        }

        public @NonNull SnapshotConsumer build() {
            final var httpClient = new HttpClient(authSupplier,
                                                  Optional.empty(),
                                                  Optional.empty());
            final var pageLoader = new PageLoader(httpClient);
            return new SnapshotConsumerImpl(httpClient,
                                            pageLoader,
                                            networkConcurrency,
                                            delayErrors);
        }
    }

    static @NonNull Builder builder() {
        return new Builder(new ArrayList<>(),
                           AuthSupplier.none(),
                           2,
                           false);
    }
}
