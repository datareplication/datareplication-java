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
    public synchronized @NonNull CompletionStage<@NonNull Optional<@NonNull PageMetadata>> get(@NonNull PageId pageId) {
        Optional<PageMetadata> result;
        result = Optional.ofNullable(contents.get(pageId));
        return CompletableFuture.supplyAsync(() -> result);
    }

    @Override
    public synchronized @NonNull CompletionStage<@NonNull List<@NonNull PageMetadata>> getWithoutNextLink() {
        List<PageMetadata> result;
        result = contents
            .values()
            .stream()
            .filter(m -> m.next().isEmpty())
            .collect(Collectors.toList());
        return CompletableFuture.supplyAsync(() -> result);
    }

    @Override
    public synchronized @NonNull CompletionStage<Void> save(@NonNull List<@NonNull PageMetadata> pages) {
        for (var page : pages) {
            contents.put(page.pageId(), page);
        }
        return CompletableFuture.supplyAsync(() -> null);
    }

    @Override
    public synchronized @NonNull CompletionStage<Void> delete(@NonNull List<@NonNull PageId> pageIds) {
        for (var pageId : pageIds) {
            contents.remove(pageId);
        }
        return CompletableFuture.supplyAsync(() -> null);
    }

    public synchronized List<PageMetadata> getAll() {
        return List.copyOf(contents.values());
    }
}
