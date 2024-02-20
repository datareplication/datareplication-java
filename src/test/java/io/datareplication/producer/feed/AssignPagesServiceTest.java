package io.datareplication.producer.feed;

import io.datareplication.model.PageId;
import io.datareplication.model.Timestamp;
import io.datareplication.model.feed.ContentId;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class AssignPagesServiceTest {
    private static class TestPageIdProvider extends RandomPageIdProvider {
        private int counter = 1;

        @Override
        PageId newPageId() {
            var pageId = PageId.of(String.format("page-%s", counter));
            counter++;
            return pageId;
        }
    }

    private final RandomPageIdProvider pageIdProvider = new TestPageIdProvider();

    private static final Instant TIMESTAMP = Instant.parse("2024-01-10T14:30:00.000Z");

    private AssignPagesService assignPagesService(long maxBytesPerPage, long maxEntitiesPerPage) {
        return new AssignPagesService(pageIdProvider, maxBytesPerPage, maxEntitiesPerPage);
    }

    @Test
    void shouldReturnEmpty_whenZeroEntities() {
        final var assignPagesService = assignPagesService(Long.MAX_VALUE, Long.MAX_VALUE);

        final var result = assignPagesService.assignPages(Optional.empty(), List.of());

        assertThat(result).isEmpty();
    }

    @Test
    void shouldAssignNewPagesByMaxBytes_whenNoLatestPage() {
        final var assignPagesService = assignPagesService(10, Long.MAX_VALUE);
        final var entity1 = unassignedEntity("1", TIMESTAMP, 4);
        final var entity2 = unassignedEntity("2", TIMESTAMP, 4);
        final var entity3 = unassignedEntity("3", TIMESTAMP.plusSeconds(1), 4);
        final var entity4 = unassignedEntity("4", TIMESTAMP.plusSeconds(10), 6);
        final var entity5 = unassignedEntity("5", TIMESTAMP.plusSeconds(17), 15);
        final var entity6 = unassignedEntity("6", TIMESTAMP.plusSeconds(35), 1);

        final var result = assignPagesService.assignPages(
            Optional.empty(),
            List.of(entity1, entity2, entity3, entity4, entity5, entity6)
        );

        assertThat(result).contains(new AssignPagesService.AssignPagesResult(
            List.of(
                assignedEntity(entity1, "page-1"),
                assignedEntity(entity2, "page-1"),
                assignedEntity(entity3, "page-2"),
                assignedEntity(entity4, "page-2"),
                assignedEntity(entity5, "page-3"),
                assignedEntity(entity6, "page-4")),
            List.of(
                new FeedPageMetadataRepository.PageMetadata(
                    PageId.of("page-1"),
                    entity2.lastModified(),
                    Optional.empty(),
                    Optional.of(PageId.of("page-2")),
                    8,
                    2,
                    Generations.INITIAL_GENERATION),
                new FeedPageMetadataRepository.PageMetadata(
                    PageId.of("page-2"),
                    entity4.lastModified(),
                    Optional.of(PageId.of("page-1")),
                    Optional.of(PageId.of("page-3")),
                    10,
                    2,
                    Generations.INITIAL_GENERATION),
                new FeedPageMetadataRepository.PageMetadata(
                    PageId.of("page-3"),
                    entity5.lastModified(),
                    Optional.of(PageId.of("page-2")),
                    Optional.of(PageId.of("page-4")),
                    15,
                    1,
                    Generations.INITIAL_GENERATION)),
            new FeedPageMetadataRepository.PageMetadata(
                PageId.of("page-4"),
                entity6.lastModified(),
                Optional.of(PageId.of("page-3")),
                Optional.empty(),
                1,
                1,
                Generations.INITIAL_GENERATION),
            Optional.empty()
        ));
    }

    @Test
    void shouldAssignNewPagesByMaxEntities_whenNoLatestPage() {
        final var assignPagesService = assignPagesService(Long.MAX_VALUE, 2);
        final var entity1 = unassignedEntity("1", TIMESTAMP, 15);
        final var entity2 = unassignedEntity("2", TIMESTAMP, 13);
        final var entity3 = unassignedEntity("3", TIMESTAMP.plusSeconds(1), 11);
        final var entity4 = unassignedEntity("4", TIMESTAMP.plusSeconds(10), 9);
        final var entity5 = unassignedEntity("5", TIMESTAMP.plusSeconds(10), 1);

        final var result = assignPagesService.assignPages(
            Optional.empty(),
            List.of(entity1, entity2, entity3, entity4, entity5)
        );

        assertThat(result).contains(new AssignPagesService.AssignPagesResult(
            List.of(
                assignedEntity(entity1, "page-1"),
                assignedEntity(entity2, "page-1"),
                assignedEntity(entity3, "page-2"),
                assignedEntity(entity4, "page-2"),
                assignedEntity(entity5, "page-3")),
            List.of(
                new FeedPageMetadataRepository.PageMetadata(
                    PageId.of("page-1"),
                    entity2.lastModified(),
                    Optional.empty(),
                    Optional.of(PageId.of("page-2")),
                    28,
                    2,
                    Generations.INITIAL_GENERATION),
                new FeedPageMetadataRepository.PageMetadata(
                    PageId.of("page-2"),
                    entity4.lastModified(),
                    Optional.of(PageId.of("page-1")),
                    Optional.of(PageId.of("page-3")),
                    20,
                    2,
                    Generations.INITIAL_GENERATION)),
            new FeedPageMetadataRepository.PageMetadata(
                PageId.of("page-3"),
                entity5.lastModified(),
                Optional.of(PageId.of("page-2")),
                Optional.empty(),
                1,
                1,
                Generations.INITIAL_GENERATION),
            Optional.empty()
        ));
    }

    @Test
    void shouldAssignNewPagesByMaxBytesAndMaxEntries_whenNoLatestPage() {
        final var assignPagesService = assignPagesService(10, 2);
        final var entity1 = unassignedEntity("1", TIMESTAMP, 4);
        final var entity2 = unassignedEntity("2", TIMESTAMP, 4);
        final var entity3 = unassignedEntity("3", TIMESTAMP.plusSeconds(1), 2);
        final var entity4 = unassignedEntity("4", TIMESTAMP.plusSeconds(10), 9);
        final var entity5 = unassignedEntity("5", TIMESTAMP.plusSeconds(17), 1);

        final var result = assignPagesService.assignPages(
            Optional.empty(),
            List.of(entity1, entity2, entity3, entity4, entity5)
        );

        assertThat(result).contains(new AssignPagesService.AssignPagesResult(
            List.of(
                assignedEntity(entity1, "page-1"),
                assignedEntity(entity2, "page-1"),
                assignedEntity(entity3, "page-2"),
                assignedEntity(entity4, "page-3"),
                assignedEntity(entity5, "page-3")),
            List.of(
                new FeedPageMetadataRepository.PageMetadata(
                    PageId.of("page-1"),
                    entity2.lastModified(),
                    Optional.empty(),
                    Optional.of(PageId.of("page-2")),
                    8,
                    2,
                    Generations.INITIAL_GENERATION),
                new FeedPageMetadataRepository.PageMetadata(
                    PageId.of("page-2"),
                    entity3.lastModified(),
                    Optional.of(PageId.of("page-1")),
                    Optional.of(PageId.of("page-3")),
                    2,
                    1,
                    Generations.INITIAL_GENERATION)),
            new FeedPageMetadataRepository.PageMetadata(
                PageId.of("page-3"),
                entity5.lastModified(),
                Optional.of(PageId.of("page-2")),
                Optional.empty(),
                10,
                2,
                Generations.INITIAL_GENERATION),
            Optional.empty()
        ));
    }


    @Test
    void shouldExtendOldLatestPage() {
        final var assignPagesService = assignPagesService(Long.MAX_VALUE, 4);
        final var latestPage = new FeedPageMetadataRepository.PageMetadata(
            PageId.of("old-latest-page"),
            Timestamp.of(TIMESTAMP.plusSeconds(-15)),
            Optional.of(PageId.of("some-other-page-entirely")),
            Optional.empty(),
            1,
            1,
            666);
        final var entity1 = unassignedEntity("1", TIMESTAMP, 1);
        final var entity2 = unassignedEntity("2", TIMESTAMP.plusSeconds(2), 1);
        final var entity3 = unassignedEntity("3", TIMESTAMP.plusSeconds(4), 1);

        final var result = assignPagesService.assignPages(
            Optional.of(latestPage),
            List.of(entity1, entity2, entity3)
        );

        assertThat(result).contains(new AssignPagesService.AssignPagesResult(
            List.of(
                assignedEntity(entity1, "old-latest-page"),
                assignedEntity(entity2, "old-latest-page"),
                assignedEntity(entity3, "old-latest-page")),
            List.of(),
            new FeedPageMetadataRepository.PageMetadata(
                latestPage.pageId(),
                entity3.lastModified(),
                latestPage.prev(),
                Optional.empty(),
                4,
                4,
                667),
            Optional.empty()
        ));
    }

    @Test
    void shouldExtendOldLatestPageAndGenerateNewPages() {
        final var assignPagesService = assignPagesService(10, Long.MAX_VALUE);
        final var latestPage = new FeedPageMetadataRepository.PageMetadata(
            PageId.of("old-latest-page"),
            Timestamp.of(TIMESTAMP.plusSeconds(-15)),
            Optional.of(PageId.of("some-other-page-entirely")),
            Optional.empty(),
            2,
            1,
            15);
        final var entity1 = unassignedEntity("1", TIMESTAMP, 4);
        final var entity2 = unassignedEntity("2", TIMESTAMP.plusSeconds(2), 4);
        final var entity3 = unassignedEntity("3", TIMESTAMP.plusSeconds(4), 10);
        final var entity4 = unassignedEntity("4", TIMESTAMP.plusSeconds(6), 8);

        final var result = assignPagesService.assignPages(
            Optional.of(latestPage),
            List.of(entity1, entity2, entity3, entity4)
        );

        assertThat(result).contains(new AssignPagesService.AssignPagesResult(
            List.of(
                assignedEntity(entity1, "old-latest-page"),
                assignedEntity(entity2, "old-latest-page"),
                assignedEntity(entity3, "page-1"),
                assignedEntity(entity4, "page-2")),
            List.of(
                new FeedPageMetadataRepository.PageMetadata(
                    PageId.of("page-1"),
                    entity3.lastModified(),
                    Optional.of(PageId.of("old-latest-page")),
                    Optional.of(PageId.of("page-2")),
                    10,
                    1,
                    16)),
            new FeedPageMetadataRepository.PageMetadata(
                PageId.of("page-2"),
                entity4.lastModified(),
                Optional.of(PageId.of("page-1")),
                Optional.empty(),
                8,
                1,
                16),
            Optional.of(new FeedPageMetadataRepository.PageMetadata(
                latestPage.pageId(),
                entity2.lastModified(),
                latestPage.prev(),
                Optional.of(PageId.of("page-1")),
                10,
                3,
                16))
        ));
    }

    @Test
    void shouldOnlyGenerateNewPages_whenOldLatestPageCannotBeFilledFurther() {
        final var assignPagesService = assignPagesService(10, Long.MAX_VALUE);
        final var latestPage = new FeedPageMetadataRepository.PageMetadata(
            PageId.of("old-latest-page"),
            Timestamp.of(TIMESTAMP.plusSeconds(-15)),
            Optional.of(PageId.of("some-other-page-entirely")),
            Optional.empty(),
            9,
            5,
            100);
        final var entity1 = unassignedEntity("1", TIMESTAMP, 2);
        final var entity2 = unassignedEntity("2", TIMESTAMP.plusSeconds(2), 7);

        final var result = assignPagesService.assignPages(
            Optional.of(latestPage),
            List.of(entity1, entity2)
        );

        assertThat(result).contains(new AssignPagesService.AssignPagesResult(
            List.of(
                assignedEntity(entity1, "page-1"),
                assignedEntity(entity2, "page-1")),
            List.of(),
            new FeedPageMetadataRepository.PageMetadata(
                PageId.of("page-1"),
                entity2.lastModified(),
                Optional.of(PageId.of("old-latest-page")),
                Optional.empty(),
                9,
                2,
                101),
            Optional.of(new FeedPageMetadataRepository.PageMetadata(
                latestPage.pageId(),
                latestPage.lastModified(),
                latestPage.prev(),
                Optional.of(PageId.of("page-1")),
                latestPage.numberOfBytes(),
                latestPage.numberOfEntities(),
                101))
        ));
    }

    private static FeedEntityRepository.PageAssignment unassignedEntity(
        String id,
        Instant lastModified,
        int contentLength
    ) {
        return new FeedEntityRepository.PageAssignment(
            ContentId.of(id),
            Timestamp.of(lastModified),
            Optional.empty(),
            contentLength,
            Optional.empty()
        );
    }

    private static FeedEntityRepository.PageAssignment assignedEntity(
        FeedEntityRepository.PageAssignment entity,
        String page
    ) {
        return new FeedEntityRepository.PageAssignment(
            entity.contentId(),
            entity.lastModified(),
            entity.originalLastModified(),
            entity.contentLength(),
            Optional.of(PageId.of(page))
        );
    }
}
