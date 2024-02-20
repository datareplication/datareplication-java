package io.datareplication.producer.feed.testhelper;

import io.datareplication.model.PageId;
import io.datareplication.producer.feed.FeedPageMetadataRepository;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

@RequiredArgsConstructor
public class FeedPageMetadataPauseRepository implements FeedPageMetadataRepository {
    private final FeedPageMetadataRepository inner;
    private final List<CompletableFuture<Void>> paused = new ArrayList<>();

    public synchronized void unpause() {
        for (var paused : paused) {
            paused.completeAsync(() -> null);
        }
        paused.clear();
    }

    public synchronized void waitForPause() throws InterruptedException {
        while (paused.isEmpty()) {
            this.wait();
        }
    }

    @Override
    public @NonNull CompletionStage<@NonNull Optional<@NonNull PageMetadata>> get(@NonNull PageId pageId) {
        return inner.get(pageId);
    }

    @Override
    public @NonNull CompletionStage<@NonNull List<@NonNull PageMetadata>> getWithoutNextLink() {
        return inner.getWithoutNextLink();
    }

    @Override
    public synchronized @NonNull CompletionStage<Void> save(@NonNull List<@NonNull PageMetadata> pages) {
        var future = new CompletableFuture<Void>();
        paused.add(future);
        this.notifyAll();
        return future.thenCompose(v -> inner.save(pages));
    }

    @Override
    public @NonNull CompletionStage<Void> delete(@NonNull List<@NonNull PageId> pageIds) {
        return inner.delete(pageIds);
    }
}
