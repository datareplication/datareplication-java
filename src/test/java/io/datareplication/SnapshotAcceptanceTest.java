package io.datareplication;

import io.datareplication.consumer.snapshot.SnapshotConsumer;
import io.datareplication.model.Body;
import io.datareplication.model.Entity;
import io.datareplication.model.HttpHeaders;
import io.datareplication.model.PageId;
import io.datareplication.model.Url;
import io.datareplication.model.snapshot.SnapshotEntityHeader;
import io.datareplication.model.snapshot.SnapshotId;
import io.datareplication.model.snapshot.SnapshotIndex;
import io.datareplication.model.snapshot.testhelper.SnapshotIndexInMemoryRepository;
import io.datareplication.model.snapshot.testhelper.SnapshotPageInMemoryRepository;
import io.datareplication.producer.snapshot.SnapshotPageUrlBuilder;
import io.datareplication.producer.snapshot.SnapshotProducer;
import io.reactivex.rxjava3.core.Flowable;
import lombok.NonNull;
import org.junit.jupiter.api.Test;
import org.reactivestreams.FlowAdapters;

import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;

public class SnapshotAcceptanceTest {

    @Test
    void shouldPublishAndConsumeSnapshot() throws ExecutionException, InterruptedException {
        //region Produce Snapshot
        Flowable<Entity<SnapshotEntityHeader>> entityFlow = Flowable
            .just("Hello", "World", "I", "am", "a", "snapshot")
            .map(this::toSnapshotEntity);
        // TODO: Create Snapshot with Snapshot Producer -> Serve via Http Server

        SnapshotIndexInMemoryRepository snapshotRepository = new SnapshotIndexInMemoryRepository();
        SnapshotPageInMemoryRepository snapshotPageRepository = new SnapshotPageInMemoryRepository();
        SnapshotPageUrlBuilder snapshotPageUrlBuilder = new SnapshotPageUrlBuilder() {
            @Override
            public @NonNull Url pageUrl(@NonNull final SnapshotId snapshotId, @NonNull final PageId pageId) {
                // TODO: Server prefix
                return Url.of(snapshotId.value() + "/" + pageId.value());
            }
        };
        SnapshotProducer snapshotProducer = SnapshotProducer
            .builder()
            // TODO: Additional configuration & use InMemoryRepositories
            .build(snapshotRepository, snapshotPageRepository, snapshotPageUrlBuilder);
        SnapshotIndex producedSnapshotIndex = snapshotProducer
            .produce(FlowAdapters.toFlowPublisher(entityFlow))
            .toCompletableFuture()
            .get();
        //endregion
        //region Consume Snapshot
        SnapshotConsumer snapshotConsumer = SnapshotConsumer
            .builder()
            // TODO: Additional configuration
            .build();
        Flowable<@NonNull Entity<@NonNull SnapshotEntityHeader>> entityFlowable =
            Flowable.fromPublisher(
                FlowAdapters.toPublisher(snapshotConsumer.streamEntities(producedSnapshotIndex))
            );
        //endregion

        //region Assert SnapshotIndex
        var snapshotIndexFromUrl = snapshotConsumer
            .loadSnapshotIndex(snapshotUrl(producedSnapshotIndex))
            .toCompletableFuture()
            .get();
        assertThat(producedSnapshotIndex).isEqualTo(snapshotIndexFromUrl);
        //endregion
        //region Assert consumed entities
        entityFlowable
            .map(entity -> entity.body().toUtf8())
            .test()
            .assertValues("Hello", "World", "I", "am", "a", "Snapshot");
        //endregion
    }

    private @NonNull Entity<@NonNull SnapshotEntityHeader> toSnapshotEntity(String content) {
        return new Entity<>(new SnapshotEntityHeader(HttpHeaders.EMPTY), Body.fromUtf8(content));
    }

    private Url snapshotUrl(SnapshotIndex index) {
        // TODO: Server configuration
        return Url.of("https://localhost:8080/" + index.id().value());
    }
}
