package io.datareplication.internal.http;

import com.github.mizosoft.methanol.Methanol;
import io.datareplication.consumer.Authorization;
import io.datareplication.consumer.HttpException;
import io.datareplication.model.HttpHeader;
import io.datareplication.model.Url;
import io.reactivex.rxjava3.core.Single;
import lombok.NonNull;

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

    public HttpClient(@NonNull AuthSupplier authSupplier,
                      @NonNull Optional<Duration> headersTimeout,
                      @NonNull Optional<Duration> readTimeout) {
        this.authSupplier = authSupplier;
        var builder = Methanol
            .newBuilder()
            .autoAcceptEncoding(true)
            .followRedirects(java.net.http.HttpClient.Redirect.NORMAL);
        headersTimeout.ifPresent(builder::headersTimeout);
        readTimeout.ifPresent(builder::readTimeout);
        this.httpClient = builder.build();
    }

    public HttpClient() {
        this(AuthSupplier.none(),
             Optional.empty(),
             Optional.empty());
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
    public <T> Single<HttpResponse<T>> get(@NonNull Url url,
                                           @NonNull HttpResponse.BodyHandler<T> bodyHandler) {
        return Single
            .fromSupplier(() -> newRequest(url))
            .map(req -> req.GET().build())
            .flatMap(request -> send(url, request, bodyHandler));
    }

    private <T> Single<HttpResponse<T>> send(Url url,
                                             HttpRequest request,
                                             HttpResponse.BodyHandler<T> bodyHandler) {
        return Single
            .fromCompletionStage(httpClient.sendAsync(request, bodyHandler))
            .onErrorResumeNext(exc -> {
                if (exc instanceof CompletionException && exc.getCause() instanceof IOException) {
                    return Single.error(new HttpException.NetworkError(url, exc.getCause()));
                } else if (exc instanceof IOException) {
                    return Single.error(new HttpException.NetworkError(url, exc));
                } else {
                    return Single.error(exc);
                }
            })
            .flatMap(response -> checkResponse(url, response));
        // TODO: retries?
    }

    private HttpRequest.Builder newRequest(Url url) {
        // TODO: additional headers
        try {
            final var req = HttpRequest.newBuilder(new URI(url.value()));
            final var authHeader = authSupplier.apply(url).map(Authorization::toHeader);
            return addHeader(req, authHeader);
        } catch (URISyntaxException | IllegalArgumentException cause) {
            throw new HttpException.InvalidUrl(url, cause);
        }
    }

    private HttpRequest.Builder addHeader(HttpRequest.Builder req, Optional<HttpHeader> maybeHeader) {
        return maybeHeader
            .map(header -> addHeader(req, header))
            .orElse(req);
    }

    private HttpRequest.Builder addHeader(HttpRequest.Builder req, HttpHeader header) {
        for (var value : header.values()) {
            req = req.header(header.name(), value);
        }
        return req;
    }

    private <T> Single<HttpResponse<T>> checkResponse(Url url, HttpResponse<T> response) {
        if (response.statusCode() >= SERVER_ERRORS) {
            return Single.error(new HttpException.ServerError(url, response.statusCode()));
        } else if (response.statusCode() >= CLIENT_ERRORS) {
            return Single.error(new HttpException.ClientError(url, response.statusCode()));
        } else {
            return Single.just(response);
        }
    }
}
