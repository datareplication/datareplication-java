package io.datareplication.model.snapshot.testhelper;

import io.datareplication.model.Body;
import io.datareplication.model.Page;
import io.datareplication.model.PageId;
import io.datareplication.model.snapshot.SnapshotEntityHeader;
import io.datareplication.model.snapshot.SnapshotId;
import io.datareplication.model.snapshot.SnapshotPageHeader;
import io.datareplication.producer.snapshot.SnapshotPageRepository;
import lombok.NonNull;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;

public class SnapshotPageInMemoryRepository implements SnapshotPageRepository {
    private final ConcurrentHashMap<
        PageId,
        Page<@NonNull SnapshotPageHeader, @NonNull SnapshotEntityHeader>> repository = new ConcurrentHashMap<>();

    @Override
    public @NonNull CompletionStage<Void> save(
        @NonNull final SnapshotId snapshotId,
        @NonNull final PageId pageId,
        @NonNull final Page<@NonNull SnapshotPageHeader, @NonNull SnapshotEntityHeader> page) {
        return CompletableFuture.supplyAsync(() -> {
            repository.put(pageId, page);
            return null;
        });
    }


    public Optional<Body> findBy(PageId id) {
        if (repository.containsKey(id)) {
            return Optional.of(repository.get(id).toMultipartBody());
        } else {
            return Optional.empty();
        }
    }
}
