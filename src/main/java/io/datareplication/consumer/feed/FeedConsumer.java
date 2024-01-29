package io.datareplication.consumer.feed;

import io.datareplication.consumer.Authorization;
import io.datareplication.consumer.StreamingPage;
import io.datareplication.internal.http.AuthSupplier;
import io.datareplication.internal.http.HttpClient;
import io.datareplication.internal.page.PageLoader;
import io.datareplication.model.Entity;
import io.datareplication.model.HttpHeader;
import io.datareplication.model.HttpHeaders;
import io.datareplication.model.Url;
import io.datareplication.model.feed.FeedEntityHeader;
import io.datareplication.model.feed.FeedPageHeader;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Flow;
import java.util.function.Supplier;

public interface FeedConsumer {
    // TODO: error handling
    @NonNull Flow.Publisher<@NonNull StreamingPage<@NonNull FeedPageHeader, @NonNull FeedEntityHeader>> streamPages(
        @NonNull Url url,
        @NonNull StartFrom startFrom);

    @NonNull Flow.Publisher<@NonNull Entity<@NonNull FeedEntityHeader>> streamEntities(@NonNull Url url,
                                                                                       @NonNull StartFrom startFrom);
    /**
     * A builder for {@link FeedConsumer}.
     *
     * <p>Each of the builder methods modifies the state of the builder and returns the same instance. Because of that,
     * builders are not thread-safe.</p>
     *
     * @see FeedConsumer#builder()
     */
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class Builder {
        // TODO: threadpool
        private final List<HttpHeader> additionalHeaders;
        private AuthSupplier authSupplier;

        /**
         * Add the given headers to every HTTP request made by this consumer. Calling this method multiple times will
         * add all the headers from all calls.
         *
         * @param headers headers to add to every HTTP request
         * @return this builder
         */
        public @NonNull FeedConsumer.Builder additionalHeaders(@NonNull HttpHeader... headers) {
            additionalHeaders.addAll(Arrays.asList(headers));
            return this;
        }

        /**
         * Use the given {@link Authorization} for every HTTP request made by this consumer.
         *
         * @param authorization the Authorization to use for HTTP requests
         * @return this builder
         */
        public @NonNull FeedConsumer.Builder authorization(@NonNull Authorization authorization) {
            return this.authorization(() -> authorization);
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
        public @NonNull FeedConsumer.Builder authorization(
            @NonNull Supplier<@NonNull Authorization> authorizationSupplier
        ) {
            authSupplier = AuthSupplier.supplier(authorizationSupplier);
            return this;
        }

        /**
         * Build a new {@link FeedConsumer} with the parameters set on this builder.
         *
         * @return a new {@link FeedConsumer}
         */
        public @NonNull FeedConsumer build() {
            final var httpClient = new HttpClient(
                authSupplier,
                HttpHeaders.of(additionalHeaders),
                Optional.empty(),
                Optional.empty()
            );
            final var pageLoader = new PageLoader(httpClient);
            final var headerLoader = new HeaderLoader(httpClient);
            final var feedCrawler = new FeedPageCrawler(headerLoader);
            final var feedPageHeaderParser = new FeedPageHeaderParser();
            return new FeedConsumerImpl(pageLoader, feedCrawler, feedPageHeaderParser);
        }
    }

    /**
     * Create a new {@link FeedConsumer.Builder} with default settings. Use the {@link FeedConsumer.Builder#build()}
     * method on the returned builder to create a {@link FeedConsumer} with the specified settings.
     *
     * @return a new builder
     */
    static @NonNull FeedConsumer.Builder builder() {
        return new FeedConsumer.Builder(new ArrayList<>(),
            AuthSupplier.none());
    }
}
