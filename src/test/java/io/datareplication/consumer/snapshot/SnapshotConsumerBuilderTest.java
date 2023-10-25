package io.datareplication.consumer.snapshot;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SnapshotConsumerBuilderTest {
    @Test
    void networkConcurrency_shouldNotAllowZero() {
        final var builder = SnapshotConsumer.builder();

        assertThatThrownBy(() -> builder.networkConcurrency(0))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void networkConcurrency_shouldNotAllowNegative() {
        final var builder = SnapshotConsumer.builder();

        assertThatThrownBy(() -> builder.networkConcurrency(-1))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
