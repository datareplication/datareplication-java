package io.datareplication.consumer.feed;

import io.datareplication.internal.page.PageLoader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Fail.fail;

@ExtendWith(MockitoExtension.class)
class FeedConsumerImplTest {
    @Mock
    private FeedCrawler feedCrawler;
    @Mock
    private PageLoader pageLoader;

    @InjectMocks
    private FeedConsumer feedConsumer;

    @Test
    void loadLatestSite_shouldConsumeOneEntry() {
        fail("impl");
    }
}
