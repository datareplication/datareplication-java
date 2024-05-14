package io.datareplication.producer.snapshot;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RandomSnapshotIdProviderTest {
    @Test
    @DisplayName("Should create a random UUID")
    void shouldCreateARandomUuid() {
        RandomSnapshotIdProvider randomSnapshotIdProvider = new RandomSnapshotIdProvider();

        assertThat(randomSnapshotIdProvider.newSnapshotId().value()).hasSize(36);
        assertThat(randomSnapshotIdProvider.newSnapshotId()).isNotEqualTo(randomSnapshotIdProvider.newSnapshotId());
    }
}
