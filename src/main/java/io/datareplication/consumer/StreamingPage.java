package io.datareplication.consumer;

import io.datareplication.model.Entity;
import io.datareplication.model.Page;
import io.datareplication.model.ToHttpHeaders;
import lombok.NonNull;

import java.util.Optional;
import java.util.concurrent.CompletionStage;

public interface StreamingPage<PageHeader extends ToHttpHeaders, EntityHeader extends ToHttpHeaders> {
    @NonNull PageHeader header();

    // TODO: error handling
    // TODO: better reader
    // TODO: content type and content length are inaccessible
    @NonNull CompletionStage<@NonNull Optional<@NonNull EntityHeader>> next();

    @NonNull CompletionStage<@NonNull Integer> read(@NonNull byte[] buffer);

    @NonNull CompletionStage<@NonNull Optional<@NonNull Entity<@NonNull EntityHeader>>> nextEntity();

    @NonNull CompletionStage<@NonNull Page<PageHeader, EntityHeader>> toCompletePage();
}
