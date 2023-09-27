package io.datareplication;

import io.datareplication.consumer.snapshot.SnapshotConsumer;
import io.datareplication.consumer.snapshot.TestEntitySubscriber;
import io.datareplication.model.Url;
import io.datareplication.model.snapshot.SnapshotIndex;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

public class SnapshotAcceptanceTest {
    private static final Url snapshotUrl = Url.of("TODO: Http Server provides the created Files");

    @Test
    void shouldPublishAndConsumeSnapshot() throws ExecutionException, InterruptedException {
        List<String> snapshotEntities = List.of("1", "2", "3");
        TestEntitySubscriber subscriber = new TestEntitySubscriber();
        // TODO: Create Snapshot with Snapshot Producer

        SnapshotConsumer consumer = SnapshotConsumer
            .builder()
            // TODO: Additional configuration
            .build();

        SnapshotIndex snapshotIndex = consumer.loadSnapshotIndex(snapshotUrl).toCompletableFuture().get();

        consumer.streamEntities(snapshotIndex).subscribe(subscriber);
        await().atMost(5, TimeUnit.SECONDS).until(subscriber::isCompleted);
        assertThat(subscriber.getConsumedEntities()).isEqualTo(snapshotEntities);
    }
}
