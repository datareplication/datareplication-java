package io.datareplication.producer.snapshot;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UUIDPageIdProviderTest {

    @Test
    @DisplayName("Should create a random UUID")
    void shouldCreateARandomUuid() {
        UUIDPageIdProvider uuidPageIdProvider = new UUIDPageIdProvider();

        assertThat(uuidPageIdProvider.newPageId().value()).hasSize(36);
        assertThat(uuidPageIdProvider.newPageId()).isNotEqualTo(uuidPageIdProvider.newPageId());
    }
}
