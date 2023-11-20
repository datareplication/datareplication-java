package io.datareplication.internal.page;

import io.datareplication.consumer.HttpException;
import io.datareplication.consumer.PageFormatException;
import io.datareplication.consumer.StreamingPage;
import io.datareplication.internal.http.HttpClient;
import io.datareplication.internal.multipart.BufferingMultipartParser;
import io.datareplication.internal.multipart.MultipartException;
import io.datareplication.internal.multipart.MultipartParser;
import io.datareplication.model.HttpHeader;
import io.datareplication.model.HttpHeaders;
import io.datareplication.model.Url;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import lombok.NonNull;
import org.reactivestreams.FlowAdapters;

import java.io.IOException;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.Flow;
import java.util.stream.Stream;

/**
 * Download a page from a URL, parse its multipart body, and return it as a {@link StreamingPage}.
 */
public class PageLoader {
    private final HttpClient httpClient;

    public PageLoader(final HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    /**
     * Load a multipart page from the given URL.
     * @param url the URL to download
     * @throws HttpException in case of HTTP errors (invalid URL, HTTP error status codes, network errors/timeouts, ...)
     * @throws PageFormatException if the HTTP response is ok, but the page response is malformed in some way (usually
     *                             missing or malformed HTTP Content-Type header since multipart parsing errors will
     *                             only start happening when we get to {@link StreamingPage})
     * @return a {@link StreamingPage} for the given URL with unparsed headers
     */
    public Single<StreamingPage<HttpHeaders, HttpHeaders>> load(Url url) {
        return httpClient
            .get(url, HttpResponse.BodyHandlers.ofPublisher())
            .map(response -> {
                final HttpHeaders pageHeader = convertHeaders(response);
                final String contentTypeString = response
                    .headers()
                    .firstValue(HttpHeader.CONTENT_TYPE)
                    .orElseThrow(PageFormatException.MissingContentTypeHeader::new);
                final MultipartContentType multipartContentType = MultipartContentType.parse(contentTypeString);
                return parseMultipartPage(url, pageHeader, multipartContentType.boundary(), response.body());
            });
    }

    private StreamingPage<HttpHeaders, HttpHeaders> parseMultipartPage(Url url,
                                                                       HttpHeaders pageHeader,
                                                                       String boundary,
                                                                       Flow.Publisher<List<ByteBuffer>> input) {
        final BufferingMultipartParser multipartParser = new BufferingMultipartParser(
            new MultipartParser(ByteBuffer.wrap(boundary.getBytes(StandardCharsets.UTF_8))));
        final ToStreamingPageChunkTransformer chunkTransformer = new ToStreamingPageChunkTransformer();
        final Flowable<StreamingPage.Chunk<HttpHeaders>> chunks = Flowable
            .fromPublisher(FlowAdapters.toPublisher(input))
            .flatMapIterable(list -> list)
            .flatMapIterable(multipartParser::parse)
            .map(chunkTransformer::transform)
            .flatMapMaybe(Maybe::fromOptional)
            .doOnComplete(multipartParser::finish)
            .onErrorResumeNext(exc -> {
                if (exc instanceof MultipartException) {
                    return Flowable.error(new PageFormatException.InvalidMultipart(exc));
                } else if (exc instanceof IOException) {
                    return Flowable.error(new HttpException.NetworkError(url, exc));
                } else {
                    return Flowable.error(exc);
                }
            });
        final Flow.Publisher<StreamingPage.Chunk<HttpHeaders>> publisher = FlowAdapters.toFlowPublisher(chunks);
        return new StreamingPage<>() {
            @Override
            public @NonNull HttpHeaders header() {
                return pageHeader;
            }

            @Override
            public @NonNull String boundary() {
                return boundary;
            }

            @Override
            public void subscribe(final Flow.Subscriber<? super Chunk<HttpHeaders>> subscriber) {
                publisher.subscribe(subscriber);
            }
        };
    }

    private HttpHeaders convertHeaders(HttpResponse<?> response) {
        final Stream<HttpHeader> headers = response
            .headers()
            .map()
            .entrySet()
            .stream()
            .map(entry -> HttpHeader.of(entry.getKey(), entry.getValue()));
        return HttpHeaders.of(headers.iterator());
    }
}
