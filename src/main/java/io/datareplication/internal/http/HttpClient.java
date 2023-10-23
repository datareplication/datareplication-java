package io.datareplication.internal.http;

import com.github.mizosoft.methanol.Methanol;
import io.datareplication.consumer.HttpException;
import io.datareplication.model.Url;
import io.reactivex.rxjava3.core.Single;
import lombok.NonNull;
import lombok.Value;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class HttpClient {
    private final Methanol httpClient;
    private final int retries;

    public HttpClient(final Methanol httpClient, final int retries) {
        this.httpClient = httpClient;
        this.retries = retries;
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
            .onErrorResumeNext(cause -> Single.error(new HttpException.NetworkError(cause)))
            .flatMap(this::checkResponse);
        // TODO: retries?
    }

    private Single<HttpRequest.Builder> newRequest(Url url) {
        // TODO: auth & additional headers
        Single<HttpRequest.Builder> result;
        try {
            result = Single.just(HttpRequest.newBuilder(URI.create(url.value())));
        } catch (Exception cause) {
            result = Single.error(new HttpException.InvalidUrl(url, cause));
        }
        return result;
    }

    private <T> Single<HttpResponse<T>> checkResponse(HttpResponse<T> response) {
        if (response.statusCode() >= 500) {
            return Single.error(new HttpException.ServerError(response.statusCode()));
        } else if (response.statusCode() >= 400) {
            return Single.error(new HttpException.ClientError(response.statusCode()));
        } else {
            return Single.just(response);
        }
    }
}
