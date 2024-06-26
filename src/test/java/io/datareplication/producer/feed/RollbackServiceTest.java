package io.datareplication.producer.feed;

import io.datareplication.model.PageId;
import io.datareplication.model.feed.ContentId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RollbackServiceTest {
    private final FeedEntityRepository feedEntityRepository = mock(FeedEntityRepository.class);
    private final FeedPageMetadataRepository feedPageMetadataRepository = mock(FeedPageMetadataRepository.class);

    private static final Duration TEST_TIMEOUT = Duration.ofSeconds(1);
    private static final Instant INSTANT_1 = Instant.parse("2023-11-09T14:13:31.000Z");
    private static final Instant INSTANT_2 = Instant.parse("2023-12-31T07:17:45.666Z");
    private static final Instant INSTANT_3 = Instant.parse("2023-04-16T03:00:00.001Z");

    private final RollbackService rollbackService = new RollbackService(
        feedEntityRepository,
        feedPageMetadataRepository
    );

    @BeforeEach
    void setUp() {
        when(feedEntityRepository.savePageAssignments(any())).thenReturn(Mono.<Void>empty().toFuture());
        when(feedPageMetadataRepository.delete(any())).thenReturn(Mono.<Void>empty().toFuture());
    }

    @Test
    void shouldDoNothing_whenOnlyModifiedLatestPageAndStateIsConsistent() {
        final var latestPage = somePageMetadata("latest", 3);
        final var entity1 = somePageAssignment("1");
        final var entity2 = somePageAssignment("2");
        final var entity3 = somePageAssignment("3");
        final var journalState = new FeedProducerJournalRepository.JournalState(
            List.of(),
            latestPage.pageId(),
            Optional.empty()
        );
        when(feedPageMetadataRepository.getWithoutNextLink())
            .thenReturn(Mono.just(List.of(latestPage)).toFuture());
        when(feedEntityRepository.getPageAssignments(latestPage.pageId()))
            .thenReturn(Mono.just(List.of(entity1, entity2, entity3)).toFuture());

        final var result = rollbackService.rollback(journalState);

        assertThat(result.toFuture()).succeedsWithin(TEST_TIMEOUT);
        verify(feedEntityRepository).savePageAssignments(List.of());
        verify(feedPageMetadataRepository, never()).save(any());
    }

    @Test
    void shouldDoNothing_whenNewLatestPageAndStateIsConsistent() {
        final var latestPage = somePageMetadata("latest", 2);
        final var entity1 = somePageAssignment("1");
        final var entity2 = somePageAssignment("2");
        final var journalState = new FeedProducerJournalRepository.JournalState(
            List.of(PageId.of("new-page-1"), PageId.of("new-page-2")),
            latestPage.pageId(),
            Optional.of(PageId.of("previous-latest"))
        );
        when(feedPageMetadataRepository.getWithoutNextLink())
            .thenReturn(Mono.just(List.of(latestPage)).toFuture());
        when(feedEntityRepository.getPageAssignments(latestPage.pageId()))
            .thenReturn(Mono.just(List.of(entity1, entity2)).toFuture());

        final var result = rollbackService.rollback(journalState);

        assertThat(result.toFuture()).succeedsWithin(TEST_TIMEOUT);
        verify(feedEntityRepository).savePageAssignments(List.of());
        verify(feedPageMetadataRepository, never()).save(any());
    }

    @Test
    void shouldUnsetNotYetVisibleEntities_whenOnlyModifiedLatestPageAndMoreEntitiesThanExpected() {
        final var latestPage = somePageMetadata("latest", 2);
        final var entity1 = somePageAssignment("1");
        final var entity2 = somePageAssignment("2");
        final var entity3 = new FeedEntityRepository.PageAssignment(
            ContentId.of("3"),
            INSTANT_1,
            Optional.of(INSTANT_2),
            31,
            Optional.of(latestPage.pageId())
        );
        final var entity4 = new FeedEntityRepository.PageAssignment(
            ContentId.of("4"),
            INSTANT_3,
            Optional.empty(),
            41,
            Optional.of(latestPage.pageId())
        );
        final var journalState = new FeedProducerJournalRepository.JournalState(
            List.of(),
            latestPage.pageId(),
            Optional.empty()
        );
        when(feedPageMetadataRepository.getWithoutNextLink())
            .thenReturn(Mono.just(List.of(latestPage)).toFuture());
        when(feedEntityRepository.getPageAssignments(latestPage.pageId()))
            .thenReturn(Mono.just(List.of(entity1, entity2, entity3, entity4)).toFuture());

        final var result = rollbackService.rollback(journalState);

        assertThat(result.toFuture()).succeedsWithin(TEST_TIMEOUT);
        verify(feedEntityRepository).savePageAssignments(List.of(
            new FeedEntityRepository.PageAssignment(
                ContentId.of("3"),
                INSTANT_2,
                Optional.empty(),
                31,
                Optional.empty()
            ),
            new FeedEntityRepository.PageAssignment(
                ContentId.of("4"),
                INSTANT_3,
                Optional.empty(),
                41,
                Optional.empty()
            )
        ));
        verify(feedPageMetadataRepository, never()).save(any());
    }

    @Test
    void shouldDeleteNewPagesAndAssignments_whenLatestPageWasNotSwitchedAndNewPagesWereCreated() {
        final var latestPage = somePageMetadata("latest", 2);
        final var latestPageEntity1 = somePageAssignment("1");
        final var latestPageEntity2 = somePageAssignment("2");
        final var newPage1 = somePageMetadata("new-1", 14);
        final var newPage2 = somePageMetadata("new-2", 15);
        final var newLatest = somePageMetadata("new-latest", 1);
        final var newPage1Entity = new FeedEntityRepository.PageAssignment(
            ContentId.of("3"),
            INSTANT_1,
            Optional.of(INSTANT_2),
            2,
            Optional.of(newPage1.pageId())
        );
        final var newPage2Entity = new FeedEntityRepository.PageAssignment(
            ContentId.of("4"),
            INSTANT_3,
            Optional.empty(),
            2,
            Optional.of(newPage2.pageId())
        );
        final var newLatestPageEntity = new FeedEntityRepository.PageAssignment(
            ContentId.of("5"),
            INSTANT_3,
            Optional.of(INSTANT_1),
            2,
            Optional.of(newLatest.pageId())
        );
        final var journalState = new FeedProducerJournalRepository.JournalState(
            List.of(newPage1.pageId(), newPage2.pageId()),
            newLatest.pageId(),
            Optional.empty()
        );
        when(feedPageMetadataRepository.getWithoutNextLink())
            .thenReturn(Mono.just(List.of(latestPage)).toFuture());
        when(feedEntityRepository.getPageAssignments(latestPage.pageId()))
            .thenReturn(Mono.just(List.of(latestPageEntity1, latestPageEntity2)).toFuture());
        when(feedEntityRepository.getPageAssignments(newPage1.pageId()))
            .thenReturn(Mono.just(List.of(newPage1Entity)).toFuture());
        when(feedEntityRepository.getPageAssignments(newPage2.pageId()))
            .thenReturn(Mono.just(List.of(newPage2Entity)).toFuture());
        when(feedEntityRepository.getPageAssignments(newLatest.pageId()))
            .thenReturn(Mono.just(List.of(newLatestPageEntity)).toFuture());

        final var result = rollbackService.rollback(journalState);

        assertThat(result.toFuture()).succeedsWithin(TEST_TIMEOUT);
        verify(feedEntityRepository).savePageAssignments(List.of(
            new FeedEntityRepository.PageAssignment(
                ContentId.of("3"),
                INSTANT_2,
                Optional.empty(),
                2,
                Optional.empty()
            ),
            new FeedEntityRepository.PageAssignment(
                ContentId.of("4"),
                INSTANT_3,
                Optional.empty(),
                2,
                Optional.empty()
            ),
            new FeedEntityRepository.PageAssignment(
                ContentId.of("5"),
                INSTANT_1,
                Optional.empty(),
                2,
                Optional.empty()
            )
        ));
        verify(feedPageMetadataRepository).delete(List.of(newPage1.pageId(), newPage2.pageId(), newLatest.pageId()));
    }

    @Test
    void shouldDeleteNewPagesAndAssignments_whenNoCurrentLatestPage() {
        final var newLatest = somePageMetadata("new-latest", 1);
        final var entity1 = new FeedEntityRepository.PageAssignment(
            ContentId.of("1"),
            INSTANT_1,
            Optional.of(INSTANT_2),
            2,
            Optional.of(newLatest.pageId())
        );
        final var entity2 = new FeedEntityRepository.PageAssignment(
            ContentId.of("2"),
            INSTANT_3,
            Optional.empty(),
            2,
            Optional.of(newLatest.pageId())
        );
        final var entity3 = new FeedEntityRepository.PageAssignment(
            ContentId.of("3"),
            INSTANT_3,
            Optional.of(INSTANT_1),
            2,
            Optional.of(newLatest.pageId())
        );
        final var journalState = new FeedProducerJournalRepository.JournalState(
            List.of(),
            newLatest.pageId(),
            Optional.empty()
        );
        when(feedPageMetadataRepository.getWithoutNextLink())
            .thenReturn(Mono.just(Collections.<FeedPageMetadataRepository.PageMetadata>emptyList()).toFuture());
        when(feedEntityRepository.getPageAssignments(newLatest.pageId()))
            .thenReturn(Mono.just(List.of(entity1, entity2, entity3)).toFuture());

        final var result = rollbackService.rollback(journalState);

        assertThat(result.toFuture()).succeedsWithin(TEST_TIMEOUT);
        verify(feedEntityRepository).savePageAssignments(List.of(
            new FeedEntityRepository.PageAssignment(
                ContentId.of("1"),
                INSTANT_2,
                Optional.empty(),
                2,
                Optional.empty()
            ),
            new FeedEntityRepository.PageAssignment(
                ContentId.of("2"),
                INSTANT_3,
                Optional.empty(),
                2,
                Optional.empty()
            ),
            new FeedEntityRepository.PageAssignment(
                ContentId.of("3"),
                INSTANT_1,
                Optional.empty(),
                2,
                Optional.empty()
            )
        ));
        verify(feedPageMetadataRepository).delete(List.of(newLatest.pageId()));
    }

    private static FeedPageMetadataRepository.PageMetadata somePageMetadata(String id, int numberOfEntities) {
        return new FeedPageMetadataRepository.PageMetadata(
            PageId.of(id),
            Instant.now(),
            Optional.empty(),
            Optional.empty(),
            1,
            numberOfEntities,
            15
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
