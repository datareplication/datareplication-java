package io.datareplication.consumer.feed;

import io.datareplication.consumer.Authorization;
import io.datareplication.consumer.HttpException;
import io.datareplication.consumer.PageFormatException;
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

/**
 * An interface for consuming a Feed provided by the {@link io.datareplication.producer.feed.FeedProducer}.
 * <p>
 * Feed pages are paginated collections of feed entities. Each page contains a list of feed entities and an optional
 * link to the next page. Feed pages are consumed by starting from the provided feed URL (which will generally point to
 * the newest page in the feed) and walking backwards until the desired starting point determined by the
 * {@link StartFrom} parameters is reached. Then the feed pages are streamed from oldest to newest until
 * the head of the feed is reached.
 * <p>
 * This interface exposes two different ways to consume feed data:
 * <ul>
 *     <li>{@link #streamEntities(Url, StartFrom)} hides the page breaks and provides a stream of
 *         fully downloaded entities in chronological order as they appear in the feed. This is usually
 *         what you want since order matters when consuming a feed. This method also provides entity-level
 *         filtering when using either {@link StartFrom.Timestamp} or {@link StartFrom.ContentId}.</li>
 *     <li>{@link #streamPages(Url, StartFrom)} instead returns a stream of {@link StreamingPage} objects.
 *         This provides more control over how the pages are consumed: for example, it allows streaming
 *         individual entity bodies (in contrast to always downloading the entire body of an entity
 *         before it is emitted) and downloading and processing multiple feed pages in parallel.
 *         Note that this is usually <em>not</em> what you want because the order of pages and entities
 *         matters for a feed.
 *         <p>
 *         Note that this method never returns partial pages. This means that where
 *         {@link #streamEntities(Url, StartFrom)} might discard entities from the first page because
 *         their timestamps are older than requested, this method will return the full first page
 *         including those entities.
 *     </li>
 * </ul>
 *
 * <p>
 * A feed consumer is created using a builder: call the {@link #builder()} method to create a new builder
 * with default settings, call the methods on {@link Builder} to customize the consumer, then call
 * {@link Builder#build()} to create a new {@link FeedConsumer} instance.
 */
public interface FeedConsumer {
    /**
     * Stream the feed pages starting from the given {@link Url}.
     * Streaming a {@link StreamingPage} will result into receiving already consumed entities
     * with an older last modified date of the current FeedPage again.
     * The {@link io.datareplication.model.feed.ContentId} from {@link StartFrom.ContentId}
     * will not respect in streamPages, use {@link #streamEntities(Url, StartFrom)} instead.
     *
     * @param url       the {@link Url} to start streaming from
     * @param startFrom the {@link StartFrom} parameter
     * @return a {@link Flow.Publisher} of {@link StreamingPage} of {@link FeedPageHeader} and {@link FeedEntityHeader}
     * @throws FeedException.FeedNotOldEnough if the last modified date of the last page is not older
     *                                        and {@link StartFrom} is not {@link StartFrom.Beginning}
     * @throws HttpException                  in case of HTTP errors (invalid URL, HTTP error status codes,
     *                                        network errors/timeouts, ...)
     * @throws PageFormatException            if the HTTP response is ok, but the page response is malformed in some
     *                                        way (usually missing or malformed HTTP Content-Type header since
     *                                        multipart parsing errors will only start happening when we get to
     *                                        {@link StreamingPage})
     */
    @NonNull
    Flow.Publisher<@NonNull StreamingPage<@NonNull FeedPageHeader, @NonNull FeedEntityHeader>> streamPages(
        @NonNull Url url,
        @NonNull StartFrom startFrom);

    /**
     * Stream the entities of the feed starting from the given {@link Url}.
     * Streaming entities will result into skipping already consumed entities
     * with an older last modified date of the current FeedPage.
     * Streaming entities will respect the {@link io.datareplication.model.feed.ContentId}
     * from {@link StartFrom.ContentId}.
     *
     * @param url       the {@link Url} to start streaming from
     * @param startFrom the {@link StartFrom} parameter
     * @return a {@link Flow.Publisher} of {@link Entity} of {@link FeedEntityHeader}
     * @throws FeedException.FeedNotOldEnough  if the last modified date of the last page is not older
     *                                         and {@link StartFrom} is not {@link StartFrom.Beginning}
     * @throws FeedException.ContentIdNotFound when using {@link StartFrom.ContentId} and the specified content ID
     *                                         was not found with the specified timestamp
     * @throws HttpException                   in case of HTTP errors (invalid URL, HTTP error status codes,
     *                                         network errors/timeouts, ...)
     * @throws PageFormatException             if the HTTP response is ok, but the page response is malformed in
     *                                         some way (usually missing or malformed HTTP Content-Type header since
     *                                         multipart parsing errors will only start happening when we get to
     *                                         {@link StreamingPage})
     */
    @NonNull
    Flow.Publisher<@NonNull Entity<@NonNull FeedEntityHeader>> streamEntities(
        @NonNull Url url,
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
            final var feedPageHeaderParser = new FeedPageHeaderParser();
            final var pageLoader = new PageLoader(httpClient);
            final var headerLoader = new HeaderLoader(httpClient, feedPageHeaderParser);
            final var feedCrawler = new FeedPageCrawler(headerLoader);
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
