package io.datareplication.producer.feed;

import io.datareplication.internal.multipart.MultipartUtils;
import io.datareplication.model.Body;
import io.datareplication.model.Entity;
import io.datareplication.model.Page;
import io.datareplication.model.PageId;
import io.datareplication.model.Timestamp;
import io.datareplication.model.Url;
import io.datareplication.model.feed.ContentId;
import io.datareplication.model.feed.FeedEntityHeader;
import io.datareplication.model.feed.FeedPageHeader;
import io.datareplication.model.feed.Link;
import io.datareplication.model.feed.OperationType;
import io.datareplication.producer.feed.testhelper.FaultRepositoryException;
import io.datareplication.producer.feed.testhelper.FeedEntityInMemoryRepository;
import io.datareplication.producer.feed.testhelper.FeedPageMetadataFaultRepository;
import io.datareplication.producer.feed.testhelper.FeedPageMetadataInMemoryRepository;
import io.datareplication.producer.feed.testhelper.FeedPageMetadataPauseRepository;
import io.datareplication.producer.feed.testhelper.FeedProducerJournalInMemoryRepository;
import lombok.NonNull;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

public class FeedProducerIntegrationTest {
    private final FeedEntityInMemoryRepository feedEntityRepository = new FeedEntityInMemoryRepository();
    private final FeedPageMetadataInMemoryRepository feedPageMetadataRepository =
            new FeedPageMetadataInMemoryRepository();
    private final FeedProducerJournalInMemoryRepository feedProducerJournalRepository =
            new FeedProducerJournalInMemoryRepository();
    private final FeedPageUrlBuilder feedPageUrlBuilder = new FeedPageUrlBuilder() {
        @Override
        public @NonNull Url pageUrl(@NonNull PageId pageId) {
            return Url.of(pageId.value());
        }
    };

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
    void shouldPublishEntitiesAndCreatePages() throws Exception {
        var feedProducer = FeedProducer
                .builder(feedEntityRepository, feedPageMetadataRepository, feedProducerJournalRepository)
                .maxEntitiesPerPage(2)
                .maxBytesPerPage(Long.MAX_VALUE)
                .build();
        var feedPageProvider = FeedPageProvider
                .builder(feedEntityRepository, feedPageMetadataRepository, feedPageUrlBuilder)
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
        assertThatFeedPageProviderHasNoPages(feedPageProvider);

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
        assertThatFeedPageProviderHasOnePage(feedPageProvider);

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
        assertThatFeedPageProviderHasBothPages(feedPageProvider);
    }

    @Test
    void shouldRollbackAndRedoChangesOnError() throws Exception {
        var faultRepository = new FeedPageMetadataFaultRepository(feedPageMetadataRepository);
        var feedProducer = FeedProducer
                .builder(feedEntityRepository, faultRepository, feedProducerJournalRepository)
                .maxEntitiesPerPage(2)
                .maxBytesPerPage(Long.MAX_VALUE)
                .build();
        var feedPageProvider = FeedPageProvider
                .builder(feedEntityRepository, faultRepository, feedPageUrlBuilder)
                .build();

        // first batch
        feedProducer.publish(entity1).toCompletableFuture().get();
        assertThat(feedProducer.assignPages())
                .succeedsWithin(TIMEOUT, InstanceOfAssertFactories.INTEGER)
                .isEqualTo(1);
        assertThatFeedPageProviderHasOnePage(feedPageProvider);

        // second batch - failure
        feedProducer.publish(entity2).toCompletableFuture().get();
        feedProducer.publish(entity3).toCompletableFuture().get();

        faultRepository.failOn(entity2.header().lastModified());
        assertThat(feedProducer.assignPages())
                .failsWithin(TIMEOUT)
                .withThrowableThat()
                .withCauseInstanceOf(FaultRepositoryException.class);

        assertThat(feedProducerJournalRepository.getBlocking()).isPresent();
        assertThat(feedEntityRepository.getAll())
                // test that all entities are assigned (even if not visible)
                .filteredOn(entity -> entity.page().isEmpty())
                .isEmpty();
        assertThatFeedPageProviderHasOnePage(feedPageProvider);

        // second batch - success
        faultRepository.succeed();
        assertThat(feedProducer.assignPages())
                .succeedsWithin(TIMEOUT, InstanceOfAssertFactories.INTEGER)
                .isEqualTo(2);
        assertThat(feedProducerJournalRepository.getBlocking()).isEmpty();
        assertThatFeedPageProviderHasBothPages(feedPageProvider);
    }

    @Test
    void shouldNotExposeIncompleteUpdates() throws Exception {
        var pauseRepository = new FeedPageMetadataPauseRepository(feedPageMetadataRepository);
        var feedProducer = FeedProducer
                .builder(feedEntityRepository, pauseRepository, feedProducerJournalRepository)
                .maxEntitiesPerPage(2)
                .maxBytesPerPage(Long.MAX_VALUE)
                .build();
        var feedPageProvider = FeedPageProvider
                .builder(feedEntityRepository, pauseRepository, feedPageUrlBuilder)
                .build();

        // first batch
        feedProducer.publish(entity1).toCompletableFuture().get();
        var future = feedProducer.assignPages();
        pauseRepository.waitForPause();
        assertThatFeedPageProviderHasNoPages(feedPageProvider);
        pauseRepository.unpause();
        pauseRepository.waitForPause();
        assertThatFeedPageProviderHasNoPages(feedPageProvider);
        pauseRepository.unpause();
        pauseRepository.waitForPause();
        // if no previous latest page, the changes become visible after the second page save operation because the final
        // one is a noop
        assertThatFeedPageProviderHasOnePage(feedPageProvider);
        pauseRepository.unpause();
        assertThat(future)
                .succeedsWithin(TIMEOUT, InstanceOfAssertFactories.INTEGER)
                .isEqualTo(1);
        assertThatFeedPageProviderHasOnePage(feedPageProvider);

        // second batch
        feedProducer.publish(entity2).toCompletableFuture().get();
        feedProducer.publish(entity3).toCompletableFuture().get();
        future = feedProducer.assignPages();
        pauseRepository.waitForPause();
        assertThatFeedPageProviderHasOnePage(feedPageProvider);
        pauseRepository.unpause();
        pauseRepository.waitForPause();
        assertThatFeedPageProviderHasOnePage(feedPageProvider);
        pauseRepository.unpause();
        pauseRepository.waitForPause();
        // with a previous latest page, changes become visible after the final page save operation
        assertThatFeedPageProviderHasOnePage(feedPageProvider);
        pauseRepository.unpause();
        assertThat(future)
                .succeedsWithin(TIMEOUT, InstanceOfAssertFactories.INTEGER)
                .isEqualTo(2);
        assertThatFeedPageProviderHasBothPages(feedPageProvider);
    }

    private void assertThatFeedPageProviderHasNoPages(FeedPageProvider feedPageProvider) throws Exception {
        assertThat(feedPageProvider.latestPageId().toCompletableFuture().get()).isEmpty();
    }

    private void assertThatFeedPageProviderHasOnePage(FeedPageProvider feedPageProvider) throws Exception {
        var maybePageId = feedPageProvider.latestPageId().toCompletableFuture().get();
        assertThat(maybePageId).isNotEmpty();
        var latestPageId = maybePageId.get();

        assertThat(feedPageProvider.page(latestPageId).toCompletableFuture().get()).contains(new Page<>(
                new FeedPageHeader(
                        entity1.header().lastModified(),
                        Link.self(feedPageUrlBuilder.pageUrl(latestPageId)),
                        Optional.empty(),
                        Optional.empty()
                ),
                MultipartUtils.defaultBoundary(latestPageId),
                List.of(entity1)
        ));
    }

    private void assertThatFeedPageProviderHasBothPages(FeedPageProvider feedPageProvider) throws Exception {
        var maybePageId = feedPageProvider.latestPageId().toCompletableFuture().get();
        assertThat(maybePageId).isNotEmpty();
        var latestPageId = maybePageId.get();

        var latestPage = feedPageProvider.page(latestPageId).toCompletableFuture().get().get();
        assertThat(latestPage).isEqualTo(new Page<>(
                new FeedPageHeader(
                        entity3.header().lastModified(),
                        Link.self(feedPageUrlBuilder.pageUrl(latestPageId)),
                        latestPage.header().prev(),
                        Optional.empty()
                ),
                MultipartUtils.defaultBoundary(latestPageId),
                List.of(entity3)
        ));
        var pageId2 = PageId.of(latestPage.header().prev().get().value().value());
        assertThat(feedPageProvider.page(pageId2).toCompletableFuture().get()).contains(new Page<>(
                new FeedPageHeader(
                        entity2.header().lastModified(),
                        Link.self(feedPageUrlBuilder.pageUrl(pageId2)),
                        Optional.empty(),
                        Optional.of(Link.next(feedPageUrlBuilder.pageUrl(latestPageId)))
                ),
                MultipartUtils.defaultBoundary(pageId2),
                List.of(entity1, entity2)
        ));
    }
}
