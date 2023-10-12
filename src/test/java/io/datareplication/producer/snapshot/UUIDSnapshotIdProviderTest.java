package io.datareplication.producer.snapshot;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UUIDSnapshotIdProviderTest {
    @Test
    @DisplayName("Should create a random UUID")
    void shouldCreateARandomUuid() {
        UUIDSnapshotIdProvider uuidPageIdProvider = new UUIDSnapshotIdProvider();

        assertThat(uuidPageIdProvider.newSnapshotId().value()).hasSize(36);
        assertThat(uuidPageIdProvider.newSnapshotId()).isNotEqualTo(uuidPageIdProvider.newSnapshotId());
    }
}
