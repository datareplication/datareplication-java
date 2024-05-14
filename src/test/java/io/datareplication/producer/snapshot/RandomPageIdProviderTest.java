package io.datareplication.producer.snapshot;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RandomPageIdProviderTest {

    @Test
    @DisplayName("Should create a random UUID")
    void shouldCreateARandomUuid() {
        RandomPageIdProvider randomPageIdProvider = new RandomPageIdProvider();

        assertThat(randomPageIdProvider.newPageId().value()).hasSize(36);
        assertThat(randomPageIdProvider.newPageId()).isNotEqualTo(randomPageIdProvider.newPageId());
    }
}
