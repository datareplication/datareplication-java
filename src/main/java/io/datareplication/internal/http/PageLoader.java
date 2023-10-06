package io.datareplication.internal.http;

import io.datareplication.consumer.PageFormatException;
import io.datareplication.consumer.StreamingPage;
import io.datareplication.internal.multipart.MultipartParser;
import io.datareplication.model.HttpHeader;
import io.datareplication.model.HttpHeaders;
import io.datareplication.model.Url;

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
        final MultipartParser parser = new MultipartParser(ByteBuffer.wrap(boundary.getBytes(StandardCharsets.UTF_8)));
        throw new RuntimeException("not implemented");
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
