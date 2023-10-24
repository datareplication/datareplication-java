package io.datareplication.consumer.snapshot;

import io.datareplication.consumer.Authorization;
import io.datareplication.consumer.StreamingPage;
import io.datareplication.model.Body;
import io.datareplication.model.Entity;
import io.datareplication.model.HttpHeader;
import io.datareplication.model.Url;
import io.datareplication.model.snapshot.SnapshotEntityHeader;
import io.datareplication.model.snapshot.SnapshotIndex;
import io.datareplication.model.snapshot.SnapshotPageHeader;
import lombok.NonNull;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;
import java.util.function.Supplier;

class SnapshotConsumerImpl implements SnapshotConsumer {
    private final HttpClient httpClient;
    private final List<HttpHeader> additionalHeaders;
    private final Supplier<Optional<Authorization>> authSupplier;

    SnapshotConsumerImpl(final HttpClient httpClient,
                         final List<HttpHeader> additionalHeaders,
                         final Supplier<Optional<Authorization>> authSupplier) {
        this.httpClient = httpClient;
        this.additionalHeaders = additionalHeaders;
        this.authSupplier = authSupplier;
    }

    @Override
    public @NonNull CompletionStage<@NonNull SnapshotIndex> loadSnapshotIndex(@NonNull final Url url) {
        final HttpRequest request = newRequest(url).GET().build();

        // TODO: tests

        // TODO: error handling
        return httpClient
            .sendAsync(request, HttpResponse.BodyHandlers.ofByteArray())
            .thenApply(this::checkResponse)
            .thenApply(response -> SnapshotIndex.fromJson(Body.fromBytes(response.body())));
    }

    @Override
    public @NonNull Flow.Publisher<
        @NonNull StreamingPage<@NonNull SnapshotPageHeader, @NonNull SnapshotEntityHeader>
        > streamPages(@NonNull final SnapshotIndex snapshotIndex) {
        // TODO
        throw new RuntimeException("not implemented");
    }

    @Override
    public @NonNull Flow.Publisher<
        @NonNull Entity<@NonNull SnapshotEntityHeader>
        > streamEntities(@NonNull final SnapshotIndex snapshotIndex) {
        // TODO
        throw new RuntimeException("not implemented");
    }

    private HttpRequest.Builder newRequest(Url url) {
        // TODO: HTTP timeouts
        // TODO: error handling
        final HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url.value()));
        for (HttpHeader header : additionalHeaders) {
            addHeader(builder, header);
        }
        authSupplier
            .get()
            .map(Authorization::toHeader)
            .ifPresent(authHeader -> addHeader(builder, authHeader));

        return builder;
    }

    private void addHeader(HttpRequest.Builder builder, HttpHeader header) {
        for (String value : header.values()) {
            builder.header(header.name(), value);
        }
    }

    private <T> HttpResponse<T>  checkResponse(HttpResponse<T> response) {
        if (response.statusCode() >= 400 && response.statusCode() <= 599) {
            // TODO: better error
            throw new RuntimeException("HTTP error");
        }
        return response;
    }
}
