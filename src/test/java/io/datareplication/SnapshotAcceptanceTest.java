package io.datareplication;

import io.datareplication.consumer.snapshot.SnapshotConsumer;
import io.datareplication.consumer.snapshot.SnapshotEntitySubscriber;
import io.datareplication.model.Url;
import io.datareplication.model.snapshot.SnapshotIndex;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;


class SnapshotAcceptanceTest {
    private static final Url SNAPSHOT_URL = Url.of("TODO: Http Server provides the created Files");

    @Test
    void shouldPublishAndConsumeSnapshot() throws ExecutionException, InterruptedException {
        List<String> snapshotEntities = List.of("1", "2", "3");
        SnapshotEntitySubscriber subscriber = new SnapshotEntitySubscriber();
        // TODO: Create Snapshot with Snapshot Producer -> Serve via Http Server

        SnapshotConsumer consumer = SnapshotConsumer
            .builder()
            // TODO: Additional configuration
            .build();

        SnapshotIndex snapshotIndex = consumer.loadSnapshotIndex(SNAPSHOT_URL).toCompletableFuture().get();

        consumer.streamEntities(snapshotIndex).subscribe(subscriber);
        await().atMost(5, TimeUnit.SECONDS).until(subscriber::hasCompleted);
        assertThat(subscriber.getConsumedEntities()).isEqualTo(snapshotEntities);
    }
}
