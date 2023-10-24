package io.datareplication.internal.http;

import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.Value;

import javax.net.ssl.SSLSession;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Collections;
import java.util.Optional;

@Value
@AllArgsConstructor
public class TestHttpResponse<T> implements HttpResponse<T> {
    int statusCode = 200;
    @NonNull HttpHeaders headers = HttpHeaders.of(Collections.emptyMap(), (a, b) -> false);
    @NonNull T body;

    @Override
    public HttpRequest request() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<HttpResponse<T>> previousResponse() {
        return Optional.empty();
    }

    @Override
    public Optional<SSLSession> sslSession() {
        return Optional.empty();
    }

    @Override
    public URI uri() {
        throw new UnsupportedOperationException();
    }

    @Override
    public HttpClient.Version version() {
        return HttpClient.Version.HTTP_1_1;
    }
}
