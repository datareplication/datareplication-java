package io.datareplication.producer.feed;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class FeedProducerBuilderTest {
    private final FeedEntityRepository feedEntityRepository = mock(FeedEntityRepository.class);
    private final FeedPageMetadataRepository feedPageMetadataRepository = mock(FeedPageMetadataRepository.class);
    private final FeedProducerJournalRepository feedProducerJournalRepository = mock(FeedProducerJournalRepository.class);

    @Test
    void assignPagesLimit_shouldNotAllowZero() {
        final var builder = FeedProducer.builder(feedEntityRepository,
                                                 feedPageMetadataRepository,
                                                 feedProducerJournalRepository);

        assertThatThrownBy(() -> builder.assignPagesLimit(0))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void assignPagesLimit_shouldNotAllowNegative() {
        final var builder = FeedProducer.builder(feedEntityRepository,
                                                 feedPageMetadataRepository,
                                                 feedProducerJournalRepository);

        assertThatThrownBy(() -> builder.assignPagesLimit(-1))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
