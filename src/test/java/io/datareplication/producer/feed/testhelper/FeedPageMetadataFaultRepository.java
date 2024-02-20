package io.datareplication.producer.feed.testhelper;

import io.datareplication.model.PageId;
import io.datareplication.model.Timestamp;
import io.datareplication.producer.feed.FeedPageMetadataRepository;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class FeedPageMetadataFaultRepository implements FeedPageMetadataRepository {
    private final FeedPageMetadataRepository inner;
    private final Set<Timestamp> fail = new HashSet<>();

    public void failOn(Timestamp... timestamps) {
        fail.addAll(Arrays.asList(timestamps));
    }

    public void succeed() {
        fail.clear();
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
    public @NonNull CompletionStage<Void> save(@NonNull List<@NonNull PageMetadata> pages) {
        var successful = pages
            .stream()
            .takeWhile(p -> !fail.contains(p.lastModified()))
            .collect(Collectors.toList());
        return inner
            .save(successful)
            .thenAccept(v -> {
                if (successful.size() < pages.size()) {
                    throw new FaultRepositoryException();
                }
            });
    }

    @Override
    public @NonNull CompletionStage<Void> delete(@NonNull List<@NonNull PageId> pageIds) {
        return inner.delete(pageIds);
    }
}
