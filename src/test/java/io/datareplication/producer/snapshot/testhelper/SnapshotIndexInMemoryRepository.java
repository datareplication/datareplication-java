package io.datareplication.producer.snapshot.testhelper;

import io.datareplication.model.snapshot.SnapshotId;
import io.datareplication.model.snapshot.SnapshotIndex;
import io.datareplication.producer.snapshot.SnapshotIndexRepository;
import lombok.NonNull;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;

public class SnapshotIndexInMemoryRepository implements SnapshotIndexRepository {
    private final ConcurrentHashMap<SnapshotId, SnapshotIndex> repository = new ConcurrentHashMap<>();

    @Override
    public @NonNull CompletionStage<Void> save(@NonNull final SnapshotIndex snapshotIndex) {
        return CompletableFuture.supplyAsync(() -> {
            repository.put(snapshotIndex.id(), snapshotIndex);
            return null;
        });
    }

    public Optional<SnapshotIndex> findBy(SnapshotId id) {
        return Optional
            .ofNullable(repository.get(id));
    }
}
