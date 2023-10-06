package io.datareplication.internal.http;

import io.datareplication.consumer.PageFormatException;
import io.datareplication.consumer.StreamingPage;
import io.datareplication.internal.multipart.BufferingParser;
import io.datareplication.internal.multipart.Elem;
import io.datareplication.internal.multipart.MultipartParser;
import io.datareplication.model.ContentType;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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
        final MultipartParser multipartParser = new MultipartParser(ByteBuffer.wrap(boundary.getBytes(StandardCharsets.UTF_8)));
        final BufferingParser bufferingParser = new BufferingParser(multipartParser::parse);
        final YetAnotherParser anotherParser = new YetAnotherParser();
        final Flowable<StreamingPage.Chunk<HttpHeaders>> chunkFlowable = Flowable.fromPublisher(FlowAdapters.toPublisher(input))
            .flatMapIterable(list -> list)
            .map(bufferingParser::feed)
            .flatMapIterable(list -> list)
            .map(anotherParser::parse)
            .flatMapMaybe(Maybe::fromOptional);
        final Flow.Publisher<StreamingPage.Chunk<HttpHeaders>> flowPublisher = FlowAdapters.toFlowPublisher(chunkFlowable);
        return new StreamingPage<>() {
            @Override
            public @NonNull HttpHeaders header() {
                return pageHeader;
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

    private static final class YetAnotherParser {
        private final List<HttpHeader> headers = new ArrayList<>();
        private ContentType contentType = null;
        private long contentLength = 0;

        private Optional<StreamingPage.Chunk<HttpHeaders>> parse(Elem multipartElem) {
            if (multipartElem instanceof Elem.Continue) {
                return Optional.empty();
            } else if (multipartElem instanceof Elem.PartBegin) {
                headers.clear();
                contentType = null;
                contentLength = 0;
                return Optional.empty();
            } else if (multipartElem instanceof Elem.Header) {
                final Elem.Header header = (Elem.Header) multipartElem;
                if (header.name().equalsIgnoreCase(HttpHeader.CONTENT_TYPE)) {
                    contentType = ContentType.of(header.value());
                } else if (header.name().equalsIgnoreCase(HttpHeader.CONTENT_LENGTH)) {
                    // TODO: error handling
                    contentLength = Long.parseLong(header.value());
                } else {
                    headers.add(HttpHeader.of(header.name(), header.value()));
                }
                return Optional.empty();
            } else if (multipartElem instanceof Elem.DataBegin) {
                // TODO: error handling
                if (contentLength == 0) {
                    throw new RuntimeException("no contentLength");
                }
                if (contentType == null) {
                    throw new RuntimeException("no contentType");
                }
                return Optional.of(StreamingPage.Chunk.header(
                    HttpHeaders.of(headers),
                    contentLength,
                    contentType
                ));
            } else if (multipartElem instanceof Elem.Data) {
                final Elem.Data data = (Elem.Data) multipartElem;
                return Optional.of(StreamingPage.Chunk.bodyChunk(data.data()));
            } else if (multipartElem instanceof Elem.PartEnd) {
                return Optional.of(StreamingPage.Chunk.bodyEnd());
            }
            throw new IllegalArgumentException(String.format("unknown subclass of Elem %s; bug?", multipartElem));
        }
    }
}
