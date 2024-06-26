package io.datareplication.producer.feed;

import io.datareplication.model.Body;
import io.datareplication.model.Entity;
import io.datareplication.model.PageId;
import io.datareplication.model.feed.ContentId;
import io.datareplication.model.feed.FeedEntityHeader;
import io.datareplication.model.feed.OperationType;
import io.datareplication.util.SettableClock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class FeedProducerImplTest {
    private final FeedEntityRepository feedEntityRepository = mock(FeedEntityRepository.class);
    private final FeedPageMetadataRepository feedPageMetadataRepository = mock(FeedPageMetadataRepository.class);
    private final FeedProducerJournalRepository feedProducerJournalRepository =
        mock(FeedProducerJournalRepository.class);
    private final SettableClock clock = new SettableClock(SOME_TIME);
    private final RandomContentIdProvider contentIdProvider = mock(RandomContentIdProvider.class);
    private final RollbackService rollbackService = mock(RollbackService.class);
    private final GenerationRotationService generationRotationService = mock(GenerationRotationService.class);
    private final EntityTimestampsService entityTimestampsService = mock(EntityTimestampsService.class);
    private final AssignPagesService assignPagesService = mock(AssignPagesService.class);

    private static final Instant SOME_TIME = Instant.parse("2023-11-28T14:00:33.123Z");
    private static final ContentId SOME_CONTENT_ID = ContentId.of("test-content-id@datareplication.io");
    private static final Duration TEST_TIMEOUT = Duration.ofSeconds(1);
    private static final int ASSIGN_PAGES_LIMIT = 10;

    private final FeedProducer feedProducer = new FeedProducerImpl(
        feedEntityRepository,
        feedPageMetadataRepository,
        feedProducerJournalRepository,
        clock,
        contentIdProvider,
        rollbackService,
        generationRotationService,
        entityTimestampsService,
        assignPagesService,
        ASSIGN_PAGES_LIMIT
    );

    @BeforeEach
    void setUp() {
        when(contentIdProvider.newContentId()).thenReturn(SOME_CONTENT_ID);
        when(generationRotationService.rotateGenerationIfNecessary(any()))
            .thenAnswer(args -> Mono.just(args.getArgument(0)));
        when(feedProducerJournalRepository.get())
            .thenReturn(Mono.just(Optional.<FeedProducerJournalRepository.JournalState>empty()).toFuture());
        when(feedProducerJournalRepository.save(any())).thenReturn(Mono.<Void>empty().toFuture());
        when(feedProducerJournalRepository.delete()).thenReturn(Mono.<Void>empty().toFuture());
        when(feedEntityRepository.savePageAssignments(any())).thenReturn(Mono.<Void>empty().toFuture());
        when(feedPageMetadataRepository.save(any())).thenReturn(Mono.<Void>empty().toFuture());
    }

    @Test
    void publish_operationType_body_shouldSaveEntityInRepository() {
        final var operationType = OperationType.PUT;
        final var body = Body.fromUtf8("test put");
        when(feedEntityRepository.append(
                new Entity<>(
                    new FeedEntityHeader(
                        SOME_TIME,
                        operationType,
                        SOME_CONTENT_ID
                    ),
                    body)
            )
        )
            .thenReturn(Mono.<Void>empty().toFuture());

        final var result = feedProducer.publish(operationType, body);

        assertThat(result).succeedsWithin(TEST_TIMEOUT);
    }

    @Test
    void publish_operationType_body_userData_shouldSaveEntityInRepository() {
        final var operationType = OperationType.DELETE;
        final var body = Body.fromUtf8("test delete");
        final var userData = "this is the user data string, innit";
        when(
            feedEntityRepository.append(
                new Entity<>(
                    new FeedEntityHeader(
                        SOME_TIME,
                        operationType,
                        SOME_CONTENT_ID
                    ),
                    body,
                    Optional.of(userData)
                )
            )
        )
            .thenReturn(Mono.<Void>empty().toFuture());

        final var result = feedProducer.publish(operationType, body, userData);

        assertThat(result).succeedsWithin(TEST_TIMEOUT);
    }

    @Test
    void publish_entity_shouldSaveEntityInRepository() {
        final var entity = new Entity<>(
            new FeedEntityHeader(
                SOME_TIME,
                OperationType.PUT,
                SOME_CONTENT_ID),
            Body.fromUtf8("some body once told me"),
            Optional.of("the world is gonna roll me"));
        when(feedEntityRepository.append(entity))
            .thenReturn(Mono.<Void>empty().toFuture());

        final var result = feedProducer.publish(entity);

        assertThat(result).succeedsWithin(TEST_TIMEOUT);
    }

    @Test
    void assignPages_shouldSaveNewPageAssignmentsWithoutPreviousLatestPage() {
        final var newLatestPage = somePageMetadata("new-latest");
        final var newPage1 = somePageMetadata("new-page-1");
        final var newPage2 = somePageMetadata("new-page-2");
        final var entity1 = somePageAssignment("1");
        final var entity2 = somePageAssignment("2");
        final var entity3 = somePageAssignment("3");
        when(feedPageMetadataRepository.getWithoutNextLink())
            .thenReturn(Mono.just(Collections.<FeedPageMetadataRepository.PageMetadata>emptyList()).toFuture());
        when(feedEntityRepository.getUnassigned(ASSIGN_PAGES_LIMIT))
            .thenReturn(Mono.just(List.of(entity1)).toFuture());
        when(assignPagesService.assignPages(Optional.empty(), List.of(entity1)))
            .thenReturn(Optional.of(new AssignPagesService.AssignPagesResult(
                List.of(entity2, entity3),
                List.of(newPage1, newPage2),
                newLatestPage,
                Optional.empty()
            )));

        final var result = feedProducer.assignPages();

        assertThat(result)
            .isCompletedWithValue(2)
            .succeedsWithin(TEST_TIMEOUT);
        verifyNoInteractions(rollbackService);
        final var inOrder = Mockito.inOrder(
            feedProducerJournalRepository,
            feedEntityRepository,
            feedPageMetadataRepository
        );
        inOrder.verify(feedProducerJournalRepository).save(new FeedProducerJournalRepository.JournalState(
            List.of(newPage1.pageId(), newPage2.pageId()),
            newLatestPage.pageId(),
            Optional.empty()
        ));
        inOrder.verify(feedEntityRepository).savePageAssignments(List.of(entity2, entity3));
        inOrder.verify(feedPageMetadataRepository).save(List.of(newPage1, newPage2));
        inOrder.verify(feedPageMetadataRepository).save(List.of(newLatestPage));
        inOrder.verify(feedPageMetadataRepository).save(List.of());
        inOrder.verify(feedProducerJournalRepository).delete();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    void assignPages_shouldSaveNewPageAssignmentsWithExistingLatestPage() {
        final var previousLatestPage = somePageMetadata("previous-latest");
        final var newLatestPage = somePageMetadata("new-latest");
        final var newPage1 = somePageMetadata("new-page-1");
        final var newPage2 = somePageMetadata("new-page-2");
        final var entity1 = somePageAssignment("1");
        final var entity2 = somePageAssignment("2");
        final var entity3 = somePageAssignment("3");
        when(feedPageMetadataRepository.getWithoutNextLink())
            .thenReturn(Mono.just(List.of(previousLatestPage)).toFuture());
        when(feedEntityRepository.getUnassigned(ASSIGN_PAGES_LIMIT))
            .thenReturn(Mono.just(List.of(entity1)).toFuture());
        when(entityTimestampsService.updateEntityTimestamps(previousLatestPage, List.of(entity1)))
            .thenReturn(List.of(entity2));
        when(assignPagesService.assignPages(Optional.of(previousLatestPage), List.of(entity2)))
            .thenReturn(Optional.of(new AssignPagesService.AssignPagesResult(
                List.of(entity2, entity3),
                List.of(newPage1, newPage2),
                newLatestPage,
                Optional.of(previousLatestPage)
            )));

        final var result = feedProducer.assignPages();

        assertThat(result)
            .isCompletedWithValue(2)
            .succeedsWithin(TEST_TIMEOUT);
        verifyNoInteractions(rollbackService);
        final var inOrder = Mockito.inOrder(
            feedProducerJournalRepository,
            feedEntityRepository,
            feedPageMetadataRepository
        );
        inOrder.verify(feedProducerJournalRepository).save(new FeedProducerJournalRepository.JournalState(
            List.of(newPage1.pageId(), newPage2.pageId()),
            newLatestPage.pageId(),
            Optional.of(previousLatestPage.pageId())
        ));
        inOrder.verify(feedEntityRepository).savePageAssignments(List.of(entity2, entity3));
        inOrder.verify(feedPageMetadataRepository).save(List.of(newPage1, newPage2));
        inOrder.verify(feedPageMetadataRepository).save(List.of(newLatestPage));
        inOrder.verify(feedPageMetadataRepository).save(List.of(previousLatestPage));
        inOrder.verify(feedProducerJournalRepository).delete();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    void assignPages_shouldSaveNewPageAssignmentsWithUpdatedLatestPage() {
        final var oldLatestPage = somePageMetadata("latest-1");
        final var updatedLatestPage = somePageMetadata("latest-2");
        final var entity1 = somePageAssignment("1");
        final var entity2 = somePageAssignment("2");
        final var entity3 = somePageAssignment("3");
        when(feedPageMetadataRepository.getWithoutNextLink())
            .thenReturn(Mono.just(List.of(oldLatestPage)).toFuture());
        when(feedEntityRepository.getUnassigned(ASSIGN_PAGES_LIMIT))
            .thenReturn(Mono.just(List.of(entity1)).toFuture());
        when(entityTimestampsService.updateEntityTimestamps(oldLatestPage, List.of(entity1)))
            .thenReturn(List.of(entity2));
        when(assignPagesService.assignPages(Optional.of(oldLatestPage), List.of(entity2)))
            .thenReturn(Optional.of(new AssignPagesService.AssignPagesResult(
                List.of(entity3),
                List.of(),
                updatedLatestPage,
                Optional.empty()
            )));

        final var result = feedProducer.assignPages();

        assertThat(result)
            .isCompletedWithValue(1)
            .succeedsWithin(TEST_TIMEOUT);
        verifyNoInteractions(rollbackService);
        final var inOrder = Mockito.inOrder(
            feedProducerJournalRepository,
            feedEntityRepository,
            feedPageMetadataRepository
        );
        inOrder.verify(feedProducerJournalRepository).save(new FeedProducerJournalRepository.JournalState(
            List.of(),
            updatedLatestPage.pageId(),
            Optional.empty()
        ));
        inOrder.verify(feedEntityRepository).savePageAssignments(List.of(entity3));
        inOrder.verify(feedPageMetadataRepository).save(List.of());
        inOrder.verify(feedPageMetadataRepository).save(List.of(updatedLatestPage));
        inOrder.verify(feedPageMetadataRepository).save(List.of());
        inOrder.verify(feedProducerJournalRepository).delete();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    void assignPages_shouldNotSaveAnythingWithNoAssignments() {
        final var latestPage = somePageMetadata("latest");
        final var entity1 = somePageAssignment("1");
        final var entity2 = somePageAssignment("2");
        when(feedPageMetadataRepository.getWithoutNextLink())
            .thenReturn(Mono.just(List.of(latestPage)).toFuture());
        when(feedEntityRepository.getUnassigned(ASSIGN_PAGES_LIMIT))
            .thenReturn(Mono.just(List.of(entity1)).toFuture());
        when(entityTimestampsService.updateEntityTimestamps(latestPage, List.of(entity1)))
            .thenReturn(List.of(entity2));
        when(assignPagesService.assignPages(Optional.of(latestPage), List.of(entity2)))
            .thenReturn(Optional.empty());

        final var result = feedProducer.assignPages();

        assertThat(result)
            .isCompletedWithValue(0)
            .succeedsWithin(TEST_TIMEOUT);
        verifyNoInteractions(rollbackService);
        verify(feedProducerJournalRepository, never()).save(any());
        verify(feedEntityRepository, never()).savePageAssignments(any());
        verify(feedPageMetadataRepository, never()).save(any());
        verify(feedProducerJournalRepository, never()).delete();
    }

    @Test
    void assignPages_shouldUseLatestPageFromGenerationRotationService() {
        final var unrotatedLatestPage = somePageMetadata("latest-unrotated");
        final var rotatedLatestPage = somePageMetadata("latest-rotated");
        final var entity1 = somePageAssignment("1");
        final var entity2 = somePageAssignment("2");
        when(feedPageMetadataRepository.getWithoutNextLink())
            .thenReturn(Mono.just(List.of(unrotatedLatestPage)).toFuture());
        when(generationRotationService.rotateGenerationIfNecessary(Optional.of(unrotatedLatestPage)))
            .thenReturn(Mono.just(Optional.of(rotatedLatestPage)));
        when(feedEntityRepository.getUnassigned(ASSIGN_PAGES_LIMIT))
            .thenReturn(Mono.just(List.<FeedEntityRepository.PageAssignment>of()).toFuture());
        when(entityTimestampsService.updateEntityTimestamps(rotatedLatestPage, List.of(entity1)))
            .thenReturn(List.of(entity2));
        when(assignPagesService.assignPages(Optional.of(rotatedLatestPage), List.of()))
            .thenReturn(Optional.empty());

        final var result = feedProducer.assignPages();

        assertThat(result)
            .isCompletedWithValue(0)
            .succeedsWithin(TEST_TIMEOUT);
    }

    @Test
    void assignPages_shouldPerformRollbackAndThenDeleteJournalState() {
        final var journalState = new FeedProducerJournalRepository.JournalState(
            List.of(PageId.of("1"), PageId.of("2")),
            PageId.of("3"),
            Optional.of(PageId.of("4"))
        );
        when(feedProducerJournalRepository.get())
            .thenReturn(Mono.just(Optional.of(journalState)).toFuture());
        when(rollbackService.rollback(journalState)).thenReturn(Mono.empty());
        when(feedPageMetadataRepository.getWithoutNextLink())
            .thenReturn(Mono.just(Collections.<FeedPageMetadataRepository.PageMetadata>emptyList()).toFuture());

        when(feedEntityRepository.getUnassigned(ASSIGN_PAGES_LIMIT))
            .thenReturn(Mono.just(List.<FeedEntityRepository.PageAssignment>of()).toFuture());
        when(assignPagesService.assignPages(Optional.empty(), List.of()))
            .thenReturn(Optional.empty());

        final var result = feedProducer.assignPages();

        assertThat(result)
            .isCompletedWithValue(0)
            .succeedsWithin(TEST_TIMEOUT);
        final var inOrder = Mockito.inOrder(feedProducerJournalRepository, rollbackService);
        inOrder.verify(feedProducerJournalRepository).get();
        inOrder.verify(rollbackService).rollback(journalState);
        inOrder.verify(feedProducerJournalRepository).delete();
        inOrder.verifyNoMoreInteractions();
    }

    private static FeedPageMetadataRepository.PageMetadata somePageMetadata(String id) {
        return new FeedPageMetadataRepository.PageMetadata(
            PageId.of(id),
            Instant.now(),
            Optional.empty(),
            Optional.empty(),
            1,
            2,
            3
        );
    }

    private static FeedEntityRepository.PageAssignment somePageAssignment(String id) {
        return new FeedEntityRepository.PageAssignment(
            ContentId.of(id),
            Instant.now(),
            Optional.empty(),
            666,
            Optional.empty()
        );
    }
}
