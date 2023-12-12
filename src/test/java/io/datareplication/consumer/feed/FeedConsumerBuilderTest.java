package io.datareplication.consumer.feed;

import io.datareplication.consumer.Authorization;
import io.datareplication.model.HttpHeader;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FeedConsumerBuilderTest {
    @Test
    void shouldCreateFeedConsumerInstance() {
        final var feedConsumer = FeedConsumer.builder()
            .authorization(Authorization.basic("foo", "bar"))
            .additionalHeaders(HttpHeader.of("user-agent", "FeedConsumerBuilderTest"))
            .build();

        assertThat(feedConsumer).isInstanceOf(FeedConsumerImpl.class);
    }
}
