package io.datareplication.producer.snapshot;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class SnapshotProducerTest {
    @Mock
    SnapshotPageRepository snapshotPageRepository;
    @Mock
    SnapshotIndexRepository snapshotIndexRepository;
    @Mock
    SnapshotPageUrlBuilder snapshotPageUrlBuilder;

    @Test
    @DisplayName("Should build a SnapshotProducer")
    void shouldBuildASnapshotProducer() {
        SnapshotProducer snapshotProducer = SnapshotProducer
            .builder(snapshotIndexRepository, snapshotPageRepository, snapshotPageUrlBuilder)
            .maxBytesPerPage(5)
            .maxEntitiesPerPage(2)
            .build();

        assertThat(snapshotProducer).isInstanceOf(SnapshotProducerImpl.class);
    }

    @Test
    @DisplayName("should throw an error if maxBytesPerPage is below minimum")
    void shouldThrowErrorWhenMaxBytesPerPageIsBelowMinimum() {
        long pointlessValueForMaxBytesPerPage = 0L;

        IllegalArgumentException thrownException = assertThrows(IllegalArgumentException.class, () ->
            SnapshotProducer
                .builder(snapshotIndexRepository, snapshotPageRepository, snapshotPageUrlBuilder)
                .maxBytesPerPage(pointlessValueForMaxBytesPerPage)
                .build()
        );
        assertEquals("maxBytesPerPage must be >= 1", thrownException.getMessage());
    }

    @Test
    @DisplayName("should throw an error if maxEntitiesPerPage is below minimum")
    void shouldThrowErrorWhenmaxEntitiesPerPageIsBelowMinimum() {
        long pointlessValueForMaxEntitiesPerPage = 0L;

        IllegalArgumentException thrownException = assertThrows(IllegalArgumentException.class, () ->
            SnapshotProducer
                .builder(snapshotIndexRepository, snapshotPageRepository, snapshotPageUrlBuilder)
                .maxEntitiesPerPage(pointlessValueForMaxEntitiesPerPage)
                .build()
        );
        assertEquals("maxEntitiesPerPage must be >= 1", thrownException.getMessage());
    }
}
