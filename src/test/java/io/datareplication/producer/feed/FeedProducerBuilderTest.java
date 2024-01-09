package io.datareplication.producer.feed;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class FeedProducerBuilderTest {
    private final FeedEntityRepository feedEntityRepository = mock(FeedEntityRepository.class);
    private final FeedPageMetadataRepository feedPageMetadataRepository = mock(FeedPageMetadataRepository.class);
    private final FeedProducerJournalRepository feedProducerJournalRepository = mock(FeedProducerJournalRepository.class);
    private final FeedProducer.Builder builder = FeedProducer.builder(feedEntityRepository, feedPageMetadataRepository, feedProducerJournalRepository);

    @Test
    void assignPagesLimit_shouldNotAllowZero() {
        assertThatThrownBy(() -> builder.assignPagesLimit(0))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void assignPagesLimit_shouldNotAllowNegative() {
        assertThatThrownBy(() -> builder.assignPagesLimit(-1))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void maxBytesPerPage_shouldNotAllowZero() {
        assertThatThrownBy(() -> builder.maxBytesPerPage(0))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void maxBytesPerPage_shouldNotAllowNegative() {
        assertThatThrownBy(() -> builder.maxBytesPerPage(-1))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void maxEntriesPerPage_shouldNotAllowZero() {
        assertThatThrownBy(() -> builder.maxEntriesPerPage(0))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void maxEntriesPerPage_shouldNotAllowNegative() {
        assertThatThrownBy(() -> builder.maxEntriesPerPage(-1))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
