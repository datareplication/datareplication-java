package io.datareplication.producer.feed;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RandomContentIdProviderTest {
    private final RandomContentIdProvider randomContentIdProvider = new RandomContentIdProvider();

    @Test
    void shouldCreateNewContentIdWithRandomUUIDv4AndSuffix() {
        final var result = randomContentIdProvider.newContentId();

        assertThat(result.value()).endsWith("-datareplication-java@datareplication.io");
        final var uuidPart = result.value().substring(0, 36);
        assertThat(UUID.fromString(uuidPart)).extracting(UUID::version).isEqualTo(4);
    }
}
