package io.datareplication.producer.feed.testhelper;

import io.datareplication.model.PageId;
import io.datareplication.producer.feed.FeedPageMetadataRepository;
import lombok.NonNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

public class FeedPageMetadataInMemoryRepository implements FeedPageMetadataRepository {
    private final Map<PageId, PageMetadata> contents = new HashMap<>();

    @Override
    public @NonNull CompletionStage<@NonNull Optional<@NonNull PageMetadata>> get(@NonNull PageId pageId) {
        Optional<PageMetadata> result;
        synchronized (this) {
            result = Optional.ofNullable(contents.get(pageId));
        }
        return CompletableFuture.supplyAsync(() -> result);
    }

    @Override
    public @NonNull CompletionStage<@NonNull List<@NonNull PageMetadata>> getWithoutNextLink() {
        List<PageMetadata> result;
        synchronized (this) {
            result = contents
                .values()
                .stream()
                .filter(m -> m.next().isEmpty())
                .collect(Collectors.toList());
        }
        return CompletableFuture.supplyAsync(() -> result);
    }

    @Override
    public @NonNull CompletionStage<Void> save(@NonNull List<@NonNull PageMetadata> pages) {
        synchronized (this) {
            for (var page : pages) {
                contents.put(page.pageId(), page);
            }
        }
        return CompletableFuture.supplyAsync(() -> null);
    }

    @Override
    public @NonNull CompletionStage<Void> delete(@NonNull List<@NonNull PageId> pageIds) {
        synchronized (this) {
            for (var pageId : pageIds) {
                contents.remove(pageId);
            }
        }
        return CompletableFuture.supplyAsync(() -> null);
    }

    public List<PageMetadata> getAll() {
        synchronized (this) {
            return List.copyOf(contents.values());
        }
    }
}
