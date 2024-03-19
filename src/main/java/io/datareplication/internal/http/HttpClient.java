package io.datareplication.internal.http;

import com.github.mizosoft.methanol.Methanol;
import io.datareplication.consumer.Authorization;
import io.datareplication.consumer.HttpException;
import io.datareplication.model.HttpHeader;
import io.datareplication.model.HttpHeaders;
import io.datareplication.model.Url;
import lombok.NonNull;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletionException;

public class HttpClient {
    private final Methanol httpClient;
    private final AuthSupplier authSupplier;

    private static final int CLIENT_ERRORS = 400;
    private static final int SERVER_ERRORS = 500;

    /**
     * @param authSupplier      provides Authorization, is called once per request
     * @param additionalHeaders additional request headers added to all requests
     * @param headersTimeout    timeout for receiving HTTP headers, infinite if empty
     * @param readTimeout       timeout for reads (network socket reads?), infinite if empty
     */
    public HttpClient(@NonNull AuthSupplier authSupplier,
                      @NonNull HttpHeaders additionalHeaders,
                      @NonNull Optional<Duration> headersTimeout,
                      @NonNull Optional<Duration> readTimeout) {
        this.authSupplier = authSupplier;
        var builder = Methanol
            .newBuilder()
            .autoAcceptEncoding(true)
            .followRedirects(java.net.http.HttpClient.Redirect.NORMAL);
        // Builder methods are side-effecting so ignoring the return value is ok
        headersTimeout.ifPresent(builder::headersTimeout);
        readTimeout.ifPresent(builder::readTimeout);
        addDefaultHeaders(builder, additionalHeaders);
        this.httpClient = builder.build();
    }

    public HttpClient() {
        this(AuthSupplier.none(),
            HttpHeaders.EMPTY,
            Optional.empty(),
            Optional.empty());
    }

    private static void addDefaultHeaders(Methanol.Builder builder, HttpHeaders additionalHeaders) {
        for (var header : additionalHeaders) {
            for (var value : header.values()) {
                builder.defaultHeader(header.name(), value);
            }
        }
    }

    /**
     * Perform a GET request. NB error handling: this method avoids throwing directly, all errors are transported in
     * the returned async result. It also checks for 4xx and 5xx status codes and turns those into errors.
     *
     * @param url         the URL to request
     * @param bodyHandler how to return the response body
     * @param <T>         the type of the response body
     * @return the response if the request was successful
     */
    @NonNull
    public <T> Mono<@NonNull HttpResponse<T>> get(@NonNull Url url,
                                                  @NonNull HttpResponse.BodyHandler<T> bodyHandler) {
        return Mono
            .fromSupplier(() -> newRequest(url))
            .map(req -> req.GET().build())
            .flatMap(request -> send(url, request, bodyHandler));
    }

    @NonNull
    public Mono<@NonNull HttpResponse<Void>> head(@NonNull Url url) {
        return Mono
            .fromSupplier(() -> newRequest(url))
            .map(req -> req.method("HEAD", HttpRequest.BodyPublishers.noBody()).build())
            .flatMap(request -> send(url, request, HttpResponse.BodyHandlers.discarding()));
    }

    private <T> Mono<HttpResponse<T>> send(Url url,
                                           HttpRequest request,
                                           HttpResponse.BodyHandler<T> bodyHandler) {
        return Mono
            .fromCompletionStage(httpClient.sendAsync(request, bodyHandler))
            .onErrorResume(exc -> {
                if (exc instanceof CompletionException && exc.getCause() instanceof IOException) {
                    return Mono.error(new HttpException.NetworkError(url, exc.getCause()));
                } else if (exc instanceof IOException) {
                    return Mono.error(new HttpException.NetworkError(url, exc));
                } else {
                    return Mono.error(exc);
                }
            })
            .flatMap(response -> checkResponse(url, response));
        // TODO: retries?
    }

    private HttpRequest.Builder newRequest(Url url) {
        HttpRequest.Builder req;
        try {
            req = HttpRequest.newBuilder(new URI(url.value()));
        } catch (URISyntaxException | IllegalArgumentException cause) {
            throw new HttpException.InvalidUrl(url, cause);
        }

        final var authHeader = authSupplier.apply(url).map(Authorization::toHeader);
        addHeader(req, authHeader);
        return req;
    }

    private void addHeader(HttpRequest.Builder req, Optional<HttpHeader> maybeHeader) {
        maybeHeader.ifPresent(header -> addHeader(req, header));
    }

    private void addHeader(HttpRequest.Builder req, HttpHeader header) {
        for (var value : header.values()) {
            req.header(header.name(), value);
        }
    }

    private <T> Mono<HttpResponse<T>> checkResponse(Url url, HttpResponse<T> response) {
        if (response.statusCode() >= SERVER_ERRORS) {
            return Mono.error(new HttpException.ServerError(url, response.statusCode()));
        } else if (response.statusCode() >= CLIENT_ERRORS) {
            return Mono.error(new HttpException.ClientError(url, response.statusCode()));
        } else {
            return Mono.just(response);
        }
    }
}
