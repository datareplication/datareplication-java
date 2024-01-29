package io.datareplication.producer.feed.testhelper;

import io.datareplication.producer.feed.FeedProducerJournalRepository;
import lombok.NonNull;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicReference;

public class FeedProducerJournalInMemoryRepository implements FeedProducerJournalRepository {
    private final AtomicReference<Optional<JournalState>> state = new AtomicReference<>(Optional.empty());

    @Override
    public @NonNull CompletionStage<Void> save(@NonNull JournalState state) {
        this.state.set(Optional.of(state));
        return CompletableFuture.supplyAsync(() -> null);
    }

    @Override
    public @NonNull CompletionStage<@NonNull Optional<@NonNull JournalState>> get() {
        return CompletableFuture.supplyAsync(state::get);
    }

    @Override
    public @NonNull CompletionStage<Void> delete() {
        state.set(Optional.empty());
        return CompletableFuture.supplyAsync(() -> null);
    }
}
