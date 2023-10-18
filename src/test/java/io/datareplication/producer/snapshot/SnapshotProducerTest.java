package io.datareplication.producer.snapshot;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class SnapshotProducerTest {

    @Test
    @DisplayName("Should build a SnapshotProducer")
    void shouldBuildASnapshotProducer(@Mock SnapshotPageRepository snapshotPageRepository,
                                      @Mock SnapshotIndexRepository snapshotIndexRepository,
                                      @Mock SnapshotPageUrlBuilder snapshotPageUrlBuilder,
                                      @Mock PageIdProvider pageIdProvider,
                                      @Mock SnapshotIdProvider snapshotIdProvider) {
        SnapshotProducer snapshotProducer = SnapshotProducer
            .builder()
            .pageIdProvider(pageIdProvider)
            .snapshotIdProvider(snapshotIdProvider)
            .maxBytesPerPage(2)
            // TODO: Additional configuration
            .build(snapshotIndexRepository, snapshotPageRepository, snapshotPageUrlBuilder);

        assertThat(snapshotProducer).isInstanceOf(SnapshotProducerImpl.class);
    }
}
