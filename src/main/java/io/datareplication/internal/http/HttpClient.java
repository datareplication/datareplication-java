package io.datareplication.internal.http;

import com.github.mizosoft.methanol.Methanol;
import io.datareplication.consumer.HttpException;
import io.datareplication.model.Url;
import io.reactivex.rxjava3.core.Single;
import lombok.NonNull;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletionException;

public class HttpClient {
    private final Methanol httpClient;

    private static final int CLIENT_ERRORS = 400;
    private static final int SERVER_ERRORS = 500;

    public HttpClient(final Methanol httpClient) {
        this.httpClient = httpClient;
    }

    @NonNull
    public <T> Single<HttpResponse<T>> get(@NonNull Url url,
                                           @NonNull HttpResponse.BodyHandler<T> bodyHandler) {
        return newRequest(url)
            .map(req -> req.GET().build())
            .flatMap(request -> send(request, bodyHandler));
    }

    private <T> Single<HttpResponse<T>> send(HttpRequest request,
                                             HttpResponse.BodyHandler<T> bodyHandler) {
        return Single
            .fromCompletionStage(httpClient.sendAsync(request, bodyHandler))
            .onErrorResumeNext(exc -> {
                if (exc instanceof CompletionException && exc.getCause() instanceof IOException) {
                    return Single.error(new HttpException.NetworkError(exc.getCause()));
                } else if (exc instanceof IOException) {
                    return Single.error(new HttpException.NetworkError(exc));
                } else {
                    return Single.error(exc);
                }
            })
            .flatMap(this::checkResponse);
        // TODO: retries?
    }

    private Single<HttpRequest.Builder> newRequest(Url url) {
        // TODO: auth & additional headers
        Single<HttpRequest.Builder> result;
        try {
            result = Single.just(HttpRequest.newBuilder(new URI(url.value())));
        } catch (URISyntaxException cause) {
            result = Single.error(new HttpException.InvalidUrl(url, cause));
        }
        return result;
    }

    private <T> Single<HttpResponse<T>> checkResponse(HttpResponse<T> response) {
        if (response.statusCode() >= SERVER_ERRORS) {
            return Single.error(new HttpException.ServerError(response.statusCode()));
        } else if (response.statusCode() >= CLIENT_ERRORS) {
            return Single.error(new HttpException.ClientError(response.statusCode()));
        } else {
            return Single.just(response);
        }
    }
}
