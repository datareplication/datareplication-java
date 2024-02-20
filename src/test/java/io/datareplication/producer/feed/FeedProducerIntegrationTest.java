package io.datareplication.producer.feed;

import io.datareplication.model.Body;
import io.datareplication.model.Entity;
import io.datareplication.model.Timestamp;
import io.datareplication.model.feed.ContentId;
import io.datareplication.model.feed.FeedEntityHeader;
import io.datareplication.model.feed.OperationType;
import io.datareplication.producer.feed.testhelper.FeedEntityInMemoryRepository;
import io.datareplication.producer.feed.testhelper.FeedPageMetadataInMemoryRepository;
import io.datareplication.producer.feed.testhelper.FeedProducerJournalInMemoryRepository;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;

public class FeedProducerIntegrationTest {
    private final FeedEntityInMemoryRepository feedEntityRepository = new FeedEntityInMemoryRepository();
    private final FeedPageMetadataInMemoryRepository feedPageMetadataRepository =
        new FeedPageMetadataInMemoryRepository();
    private final FeedProducerJournalInMemoryRepository feedProducerJournalRepository =
        new FeedProducerJournalInMemoryRepository();

    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    private final Entity<FeedEntityHeader> entity1 = new Entity<>(
        new FeedEntityHeader(
            Timestamp.of(Instant.parse("2024-01-29T16:00:00Z")),
            OperationType.PUT,
            ContentId.of("contentId1")
        ),
        Body.fromUtf8("entity1")
    );
    private final Entity<FeedEntityHeader> entity2 = new Entity<>(
        new FeedEntityHeader(
            Timestamp.of(Instant.parse("2024-01-29T16:10:15Z")),
            OperationType.DELETE,
            ContentId.of("contentId2")
        ),
        Body.fromUtf8("entity2")
    );
    private final Entity<FeedEntityHeader> entity3 = new Entity<>(
        new FeedEntityHeader(
            Timestamp.of(Instant.parse("2024-01-29T16:20:25Z")),
            OperationType.PUT,
            ContentId.of("contentId3")
        ),
        Body.fromUtf8("entity3")
    );

    @Test
    void shouldPublishEntitiesAndCreatePages() throws ExecutionException, InterruptedException {
        var feedProducer = FeedProducer
            .builder(feedEntityRepository, feedPageMetadataRepository, feedProducerJournalRepository)
            .maxEntitiesPerPage(2)
            .maxBytesPerPage(Long.MAX_VALUE)
            .build();

        // first batch
        feedProducer.publish(entity1).toCompletableFuture().get();
        assertThat(feedEntityRepository.getAll()).containsExactlyInAnyOrder(
            new FeedEntityInMemoryRepository.FeedEntityRecord(
                entity1,
                Optional.empty(),
                Optional.empty()
            )
        );

        assertThat(feedProducer.assignPages())
            .succeedsWithin(TIMEOUT, InstanceOfAssertFactories.INTEGER)
            .isEqualTo(1);

        var pages1 = feedPageMetadataRepository.getAll();
        assertThat(pages1).hasSize(1);
        var pageId1 = pages1.get(0).pageId();
        assertThat(pages1).containsExactlyInAnyOrder(
            new FeedPageMetadataRepository.PageMetadata(
                pageId1,
                entity1.header().lastModified(),
                Optional.empty(),
                Optional.empty(),
                7,
                1,
                Generations.INITIAL_GENERATION
            )
        );
        assertThat(feedEntityRepository.getAll()).containsExactlyInAnyOrder(
            new FeedEntityInMemoryRepository.FeedEntityRecord(
                entity1,
                Optional.of(pageId1),
                Optional.empty()
            )
        );

        // second batch
        feedProducer.publish(entity2).toCompletableFuture().get();
        feedProducer.publish(entity3).toCompletableFuture().get();
        assertThat(feedEntityRepository.getAll()).containsExactlyInAnyOrder(
            new FeedEntityInMemoryRepository.FeedEntityRecord(
                entity1,
                Optional.of(pageId1),
                Optional.empty()
            ),
            new FeedEntityInMemoryRepository.FeedEntityRecord(
                entity2,
                Optional.empty(),
                Optional.empty()
            ),
            new FeedEntityInMemoryRepository.FeedEntityRecord(
                entity3,
                Optional.empty(),
                Optional.empty()
            )
        );

        assertThat(feedProducer.assignPages())
            .succeedsWithin(TIMEOUT, InstanceOfAssertFactories.INTEGER)
            .isEqualTo(2);

        var pages2 = feedPageMetadataRepository.getAll();
        assertThat(pages2).hasSize(2);
        var pageId2 = pages2.stream().filter(p -> !p.pageId().equals(pageId1)).findFirst().get().pageId();
        assertThat(pages2).containsExactlyInAnyOrder(
            new FeedPageMetadataRepository.PageMetadata(
                pageId1,
                entity2.header().lastModified(),
                Optional.empty(),
                Optional.of(pageId2),
                14,
                2,
                Generations.INITIAL_GENERATION + 1
            ),
            new FeedPageMetadataRepository.PageMetadata(
                pageId2,
                entity3.header().lastModified(),
                Optional.of(pageId1),
                Optional.empty(),
                7,
                1,
                Generations.INITIAL_GENERATION + 1
            )
        );
        assertThat(feedEntityRepository.getAll()).containsExactlyInAnyOrder(
            new FeedEntityInMemoryRepository.FeedEntityRecord(
                entity1,
                Optional.of(pageId1),
                Optional.empty()
            ),
            new FeedEntityInMemoryRepository.FeedEntityRecord(
                entity2,
                Optional.of(pageId1),
                Optional.empty()
            ),
            new FeedEntityInMemoryRepository.FeedEntityRecord(
                entity3,
                Optional.of(pageId2),
                Optional.empty()
            )
        );
    }
}
