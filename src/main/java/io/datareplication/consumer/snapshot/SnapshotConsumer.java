package io.datareplication.consumer.snapshot;

import io.datareplication.consumer.Authorization;
import io.datareplication.consumer.ConsumerException;
import io.datareplication.consumer.HttpException;
import io.datareplication.consumer.PageFormatException;
import io.datareplication.consumer.StreamingPage;
import io.datareplication.internal.http.AuthSupplier;
import io.datareplication.internal.http.HttpClient;
import io.datareplication.internal.page.PageLoader;
import io.datareplication.model.Body;
import io.datareplication.model.Entity;
import io.datareplication.model.HttpHeader;
import io.datareplication.model.HttpHeaders;
import io.datareplication.model.Url;
import io.datareplication.model.snapshot.SnapshotEntityHeader;
import io.datareplication.model.snapshot.SnapshotIndex;
import io.datareplication.model.snapshot.SnapshotPageHeader;
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

/**
 * <p>A <code>SnapshotConsumer</code> allows downloading and consuming the entities of a snapshot, given the URL of a
 * snapshot index.</p>
 *
 * <p>Consuming a snapshot is a two-step process:
 * <ol>
 *     <li>download the snapshot index using {@link #loadSnapshotIndex(Url)}</li>
 *     <li>pass the returned {@link SnapshotIndex} to either {@link #streamPages(SnapshotIndex)} or
 *         {@link #streamEntities(SnapshotIndex)} to download and stream the actual pages</li>
 * </ol>
 * Which of the two <code>stream*</code> methods to use depends on your requirements:
 * <ul>
 *     <li>{@link #streamEntities(SnapshotIndex)} hides the page breaks and provides a flat stream of fully
 *         downloaded entities. This is the recommended method because the returned entities can be directly
 *         parsed and e.g. stored in a database, which is usually the level of abstraction consumers are
 *         working at.</li>
 *     <li>{@link #streamPages(SnapshotIndex)} returns a stream of {@link StreamingPage} objects instead of
 *         entities. These allow access to page headers and allow incremental processing of entity bodies
 *         (compared to always downloading an entity in full before processing it). This interface provides
 *         more fine-grained control over how pages are downloaded, is more difficult to use than the flat
 *         stream of entities.</li>
 * </ul>
 *
 * <p>A snapshot consumer is created using a builder: call the {@link #builder()} method to create a new builder
 * with default settings, call the methods on {@link Builder} to customize the consumer, then call
 * {@link Builder#build()} to create a new {@link SnapshotConsumer} instance.</p>
 */
public interface SnapshotConsumer {
    /**
     * Download and parse the snapshot index at the given URL.
     *
     * @param url the URL to download
     * @return a {@link SnapshotIndex}
     * @throws HttpException                  in case of HTTP errors (invalid URL, HTTP error status codes,
     *                                        network errors/timeouts, ...)
     * @throws SnapshotIndex.ParsingException when the URL's content could not be parsed as a snapshot index
     *                                        (invalid JSON or missing fields)
     * @see SnapshotIndex#fromJson(Body)
     */
    @NonNull CompletionStage<@NonNull SnapshotIndex> loadSnapshotIndex(@NonNull Url url);

    /**
     * <p>Return a stream of pages in the given {@link SnapshotIndex}. The page bodies are not downloaded immediately.
     * Only the HTTP headers are downloaded initially. The returned {@link StreamingPage} objects allow incrementally
     * streaming the page's entities. They also provide methods to consume the page as a stream of entities and to
     * download the entire page into memory at once.</p>
     *
     * <p>The number of pages that are requested concurrently can be set with the
     * {@link Builder#networkConcurrency(int)} setting on the builder. If concurrency is 1, pages will be returned
     * in the order they are listed in the snapshot index. If concurrency is &gt;1, pages may be returned
     * out-of-order.</p>
     *
     * <p>By default, in an error occurs while requesting a page, the stream is terminated with that error. If the
     * {@link Builder#delayErrors(boolean)} setting is set to true, errors will be collected and returned in
     * a single batch once the stream is exhausted.</p>
     *
     * @param snapshotIndex the snapshot index to stream pages from
     * @return a stream of {@link StreamingPage} for every page URL listed in the snapshot index
     * @throws HttpException                     in case of HTTP errors (invalid URL,
     *                                           HTTP error status codes,
     *                                           network errors/timeouts, ...)
     * @throws PageFormatException               if the <code>Content-Type</code> HTTP header is missing or invalid.
     *                                           Invalid page bodies (e.g. invalid multipart documents) are not reported
     *                                           by this method directly because it doesn't parse page bodies.
     *                                           Errors in page bodies are only reported when the {@link StreamingPage}s
     *                                           themselves are consumed.
     * @throws ConsumerException.CollectedErrors if {@link Builder#delayErrors(boolean)} is true and more than one
     *                                           error occurred
     * @see StreamingPage#toCompleteEntities()
     * @see StreamingPage#toCompletePage()
     */
    @NonNull Flow.Publisher<
        @NonNull StreamingPage<@NonNull SnapshotPageHeader, @NonNull SnapshotEntityHeader>
        > streamPages(@NonNull SnapshotIndex snapshotIndex);

    /**
     * <p>Return a stream of entities in the given {@link SnapshotIndex}. This builds on
     * {@link #streamPages(SnapshotIndex)} turning each page into a stream of entities. Individual entity bodies
     * are downloaded completely before being returned.</p>
     *
     * <p>The number of pages that are downloaded concurrently can be set with the
     * {@link Builder#networkConcurrency(int)} setting on the builder. If concurrency is 1, entities will be
     * returned in the order they appear in their page, and the blocks of entities for a page will be
     * returned in the order the pages are listed in the snapshot index. If concurrency is &gt;1, entities
     * may be returned out-of-order.</p>
     *
     * <p>By default, in an error occurs while downloading a page, the stream is terminated with that error. If the
     * {@link Builder#delayErrors(boolean)} setting is set to true, errors will be collected and returned in
     * a single batch once the stream is exhausted.</p>
     *
     * @param snapshotIndex the snapshot index to stream pages from
     * @return a stream of {@link Entity} containing all entities from all pages listed in the snapshot index
     * @throws HttpException                     in case of HTTP errors (invalid URL,
     *                                           HTTP error status codes, network errors/timeouts, ...)
     * @throws PageFormatException               if the <code>Content-Type</code> HTTP header is missing or invalid,
     *                                           or if a page body is unparseable or otherwise invalid
     * @throws ConsumerException.CollectedErrors if {@link Builder#delayErrors(boolean)} is true and more than one
     *                                           error occurred
     * @see #streamPages(SnapshotIndex)
     * @see StreamingPage#toCompleteEntities()
     */
    @NonNull Flow.Publisher<
        @NonNull Entity<@NonNull SnapshotEntityHeader>
        > streamEntities(@NonNull SnapshotIndex snapshotIndex);

    /**
     * A builder for {@link SnapshotConsumer}.
     *
     * <p>Each of the builder methods modifies the state of the builder and returns the same instance. Because of that,
     * builders are not thread-safe.</p>
     *
     * @see SnapshotConsumer#builder()
     */
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class Builder {
        private final List<HttpHeader> additionalHeaders;
        private AuthSupplier authSupplier;
        private int networkConcurrency;
        private boolean delayErrors;

        // TODO: HTTP timeouts

        /**
         * Add the given headers to every HTTP request made by this consumer. Calling this method multiple times will
         * add all the headers from all calls.
         *
         * @param headers headers to add to every HTTP request
         * @return this builder
         */
        public @NonNull Builder additionalHeaders(@NonNull HttpHeader... headers) {
            additionalHeaders.addAll(Arrays.asList(headers));
            return this;
        }

        /**
         * Use the given {@link Authorization} for every HTTP request made by this consumer.
         *
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
         * in a {@link ConsumerException.CollectedErrors} object.</p>
         *
         * @param delayErrors when true, delay all errors until the entire snapshot has been processed
         * @return the builder
         */
        public @NonNull Builder delayErrors(boolean delayErrors) {
            this.delayErrors = delayErrors;
            return this;
        }

        /**
         * Build a new {@link SnapshotConsumer} with the parameters set on this builder.
         *
         * @return a new {@link SnapshotConsumer}
         */
        public @NonNull SnapshotConsumer build() {
            final var httpClient = new HttpClient(authSupplier,
                                                  HttpHeaders.of(additionalHeaders),
                                                  Optional.empty(),
                                                  Optional.empty());
            final var pageLoader = new PageLoader(httpClient);
            return new SnapshotConsumerImpl(httpClient,
                                            pageLoader,
                                            networkConcurrency,
                                            delayErrors);
        }
    }

    /**
     * Create a new {@link SnapshotConsumer.Builder} with default settings. Use the {@link Builder#build()} method on
     * the returned builder to create a {@link SnapshotConsumer} with the specified settings.
     *
     * @return a new builder
     */
    static @NonNull Builder builder() {
        return new Builder(new ArrayList<>(),
                           AuthSupplier.none(),
                           2,
                           false);
    }
}
