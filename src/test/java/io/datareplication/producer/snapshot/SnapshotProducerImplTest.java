package io.datareplication.producer.snapshot;

import io.datareplication.model.Timestamp;
import io.datareplication.model.snapshot.SnapshotId;
import io.datareplication.model.snapshot.SnapshotIndex;
import io.reactivex.rxjava3.core.Flowable;
import lombok.NonNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.reactivestreams.FlowAdapters;

import java.time.Clock;
import java.time.ZoneId;
import java.util.Collections;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SnapshotProducerImplTest {

    @Test
    @DisplayName("should produce a snapshot without entries")
    void shouldProduceEmptySnapshot(@Mock SnapshotPageRepository snapshotPageRepository,
                                    @Mock SnapshotIndexRepository snapshotIndexRepository,
                                    @Mock SnapshotPageUrlBuilder snapshotPageUrlBuilder,
                                    @Mock PageIdProvider pageIdProvider,
                                    @Mock SnapshotIdProvider snapshotIdProvider)
        throws ExecutionException, InterruptedException {
        final @NonNull SnapshotId id = SnapshotId.of("1234");
        final @NonNull Timestamp createdAt = Timestamp.now();
        when(snapshotIdProvider.newSnapshotId()).thenReturn(id);
        SnapshotProducer snapshotProducer = SnapshotProducer
            .builder()
            .pageIdProvider(pageIdProvider)
            .snapshotIdProvider(snapshotIdProvider)
            .maxWeightPerPage(2)
            // TODO: Additional configuration
            .build(snapshotIndexRepository,
                snapshotPageRepository,
                snapshotPageUrlBuilder,
                Clock.fixed(createdAt.value(), ZoneId.systemDefault()));

        CompletionStage<@NonNull SnapshotIndex> produce =
            snapshotProducer.produce(FlowAdapters.toFlowPublisher(Flowable.empty()));

        SnapshotIndex snapshotIndex = produce.toCompletableFuture().get();
        assertThat(snapshotIndex)
            .isEqualTo(new SnapshotIndex(id, createdAt, Collections.emptyList()));
    }
}
