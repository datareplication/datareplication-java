package io.datareplication.producer.feed;

import io.datareplication.model.Body;
import io.datareplication.model.Entity;
import io.datareplication.model.Timestamp;
import io.datareplication.model.feed.ContentId;
import io.datareplication.model.feed.FeedEntityHeader;
import io.datareplication.model.feed.OperationType;
import io.datareplication.util.SettableClock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FeedProducerImplTest {
    private final FeedEntityRepository feedEntityRepository = mock(FeedEntityRepository.class);
    private final FeedPageMetadataRepository feedPageMetadataRepository = mock(FeedPageMetadataRepository.class);
    private final FeedProducerJournalRepository feedProducerJournalRepository = mock(FeedProducerJournalRepository.class);
    private final SettableClock clock = new SettableClock(SOME_TIME);
    private final RandomContentIdProvider contentIdProvider = mock(RandomContentIdProvider.class);
    private final NewEntityTimestampsService newEntityTimestampsService = mock(NewEntityTimestampsService.class);
    private final AssignPagesService assignPagesService = mock(AssignPagesService.class);

    private static final Instant SOME_TIME = Instant.parse("2023-11-28T14:00:33.123Z");
    private static final ContentId SOME_CONTENT_ID = ContentId.of("test-content-id@datareplication.io");
    private static final Duration TEST_TIMEOUT = Duration.ofSeconds(1);
    private static final int ASSIGN_PAGES_LIMIT = 10;

    private final FeedProducer feedProducer = new FeedProducerImpl(feedEntityRepository,
                                                                   feedPageMetadataRepository,
                                                                   feedProducerJournalRepository,
                                                                   clock,
                                                                   contentIdProvider,
                                                                   newEntityTimestampsService,
                                                                   assignPagesService,
                                                                   ASSIGN_PAGES_LIMIT);

    @BeforeEach
    void setUp() {
        when(contentIdProvider.newContentId()).thenReturn(SOME_CONTENT_ID);
    }

    @Test
    void publish_operationType_body_shouldSaveEntityInRepository() {
        final var operationType = OperationType.PUT;
        final var body = Body.fromUtf8("test put");
        when(feedEntityRepository.append(new Entity<>(new FeedEntityHeader(Timestamp.of(SOME_TIME),
                                                                           operationType,
                                                                           SOME_CONTENT_ID),
                                                      body)))
            .thenReturn(Mono.<Void>empty().toFuture());

        final var result = feedProducer.publish(operationType, body);

        assertThat(result).succeedsWithin(TEST_TIMEOUT);
    }

    @Test
    void publish_operationType_body_userData_shouldSaveEntityInRepository() {
        final var operationType = OperationType.DELETE;
        final var body = Body.fromUtf8("test delete");
        final var userData = "this is the user data string, innit";
        when(feedEntityRepository.append(new Entity<>(new FeedEntityHeader(Timestamp.of(SOME_TIME),
                                                                           operationType,
                                                                           SOME_CONTENT_ID),
                                                      body,
                                                      Optional.of(userData))))
            .thenReturn(Mono.<Void>empty().toFuture());

        final var result = feedProducer.publish(operationType, body, userData);

        assertThat(result).succeedsWithin(TEST_TIMEOUT);
    }

    @Test
    void publish_entity_shouldSaveEntityInRepository() {
        final var entity = new Entity<>(new FeedEntityHeader(Timestamp.of(SOME_TIME),
                                                             OperationType.PUT,
                                                             SOME_CONTENT_ID),
                                        Body.fromUtf8("some body once told me"),
                                        Optional.of("the world is gonna roll me"));
        when(feedEntityRepository.append(entity))
            .thenReturn(Mono.<Void>empty().toFuture());

        final var result = feedProducer.publish(entity);

        assertThat(result).succeedsWithin(TEST_TIMEOUT);
    }
}
