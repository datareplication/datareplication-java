package io.datareplication.internal.page;

import io.datareplication.consumer.PageFormatException;
import io.datareplication.consumer.StreamingPage;
import io.datareplication.internal.multipart.BufferingMultipartParser;
import io.datareplication.internal.multipart.MultipartParser;
import io.datareplication.model.Entity;
import io.datareplication.model.HttpHeader;
import io.datareplication.model.HttpHeaders;
import io.datareplication.model.Page;
import io.datareplication.model.Url;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import lombok.NonNull;
import org.reactivestreams.FlowAdapters;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;
import java.util.stream.Stream;

public class PageLoader {
    private final HttpClient httpClient;

    public PageLoader(final HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public CompletionStage<StreamingPage<HttpHeaders, HttpHeaders>> load(Url url) {
        final HttpRequest request = newRequest(url).GET().build();

        // TODO: test test test

        return httpClient
            .sendAsync(request, HttpResponse.BodyHandlers.ofPublisher())
            .thenApply(this::checkResponse)
            .thenApply(response -> {
                final HttpHeaders pageHeader = convertHeaders(response);
                final String contentTypeString = response
                    .headers()
                    .firstValue(HttpHeader.CONTENT_TYPE)
                    .orElseThrow(PageFormatException.MissingContentTypeHeader::new);
                final MultipartContentType multipartContentType = MultipartContentType.parse(contentTypeString);
                return parseMultipartPage(pageHeader, multipartContentType.boundary(), response.body());
            });
    }

    private StreamingPage<HttpHeaders, HttpHeaders> parseMultipartPage(HttpHeaders pageHeader,
                                                                       String boundary,
                                                                       Flow.Publisher<List<ByteBuffer>> input) {
        final BufferingMultipartParser multipartParser = new BufferingMultipartParser(
            new MultipartParser(ByteBuffer.wrap(boundary.getBytes(StandardCharsets.UTF_8))));
        final ToStreamingPageChunkTransformer chunkTransformer = new ToStreamingPageChunkTransformer();
        // TODO: error handling
        final Flowable<StreamingPage.Chunk<HttpHeaders>> chunkFlowable = Flowable
            .fromPublisher(FlowAdapters.toPublisher(input))
            .flatMapIterable(list -> list)
            .flatMapIterable(multipartParser::parse)
            .map(chunkTransformer::transform)
            .flatMapMaybe(Maybe::fromOptional);
        // TODO: have to call multipartParser.isFinished when we run out of input to check for completeness
        final Flow.Publisher<StreamingPage.Chunk<HttpHeaders>> flowPublisher = FlowAdapters.toFlowPublisher(chunkFlowable);
        return new StreamingPage<>() {
            @Override
            public @NonNull HttpHeaders header() {
                return pageHeader;
            }

            @Override
            public Flow.@NonNull Publisher<@NonNull Entity<HttpHeaders>> toCompleteEntities() {
                final ToCompleteEntitiesTransformer<HttpHeaders> transformer = new ToCompleteEntitiesTransformer<>();
                final Flowable<Entity<HttpHeaders>> flowable = Flowable
                    .fromPublisher(FlowAdapters.toPublisher(this))
                    .map(transformer::transform)
                    .flatMapMaybe(Maybe::fromOptional);
                return FlowAdapters.toFlowPublisher(flowable);
            }

            @Override
            public @NonNull CompletionStage<@NonNull Page<HttpHeaders, HttpHeaders>> toCompletePage() {
                // TODO:
                throw new RuntimeException("not implemented");
            }

            @Override
            public void subscribe(final Flow.Subscriber<? super Chunk<HttpHeaders>> subscriber) {
                flowPublisher.subscribe(subscriber);
            }
        };
    }

    private HttpRequest.Builder newRequest(Url url) {
        // TODO: HTTP timeouts
        // TODO: error handling
        // TODO: auth & additional headers
        return HttpRequest.newBuilder(URI.create(url.value()));
    }

    private <T> HttpResponse<T> checkResponse(HttpResponse<T> response) {
        if (response.statusCode() >= 400 && response.statusCode() <= 599) {
            // TODO: better error
            throw new RuntimeException("HTTP error");
        }
        return response;
    }

    private HttpHeaders convertHeaders(HttpResponse<?> response) {
        final Stream<HttpHeader> x = response
            .headers()
            .map()
            .entrySet()
            .stream()
            .map(entry -> HttpHeader.of(entry.getKey(), entry.getValue()));
        return HttpHeaders.of(x.iterator());
    }
}
