package io.datareplication.producer.feed;

import io.datareplication.model.PageId;
import io.datareplication.model.Timestamp;
import io.datareplication.model.feed.ContentId;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class EntityTimestampsServiceTest {
    private final EntityTimestampsService entityTimestampsService = new EntityTimestampsService();

    private static final Instant TIMESTAMP = Instant.parse("2024-01-09T12:00:00.000Z");

    @Test
    void shouldReturnEmptyList_whenEmptyList() {
        final var latest = latestPage(TIMESTAMP);

        final var result = entityTimestampsService.updateEntityTimestamps(latest, List.of());

        assertThat(result).isEmpty();
    }

    @Test
    void shouldReturnEntitiesUnchanged_whenAllEntitiesHaveTheSameTimestampAsLatestPage() {
        final var latest = latestPage(TIMESTAMP);
        final var entity1 = entity("1", TIMESTAMP);
        final var entity2 = entity("2", TIMESTAMP);
        final var entity3 = entity("3", TIMESTAMP);

        final var result = entityTimestampsService.updateEntityTimestamps(latest, List.of(entity1, entity2, entity3));

        assertThat(result).containsExactly(entity1, entity2, entity3);
    }

    @Test
    void shouldReturnEntitiesUnchanged_whenAllEntitiesAreNewerThanLatestPage() {
        final var latest = latestPage(TIMESTAMP);
        final var entity1 = entity("1", TIMESTAMP.plusSeconds(1));
        final var entity2 = entity("2", TIMESTAMP.plusSeconds(2));
        final var entity3 = entity("3", TIMESTAMP.plusSeconds(3));

        final var result = entityTimestampsService.updateEntityTimestamps(latest, List.of(entity1, entity2, entity3));

        assertThat(result).containsExactly(entity1, entity2, entity3);
    }

    @Test
    void shouldReturnEntitiesUnchanged_whenAllEntitiesAtLeastHaveTheSameTimestampAsLatestPage() {
        final var latest = latestPage(TIMESTAMP);
        final var entity1 = entity("1", TIMESTAMP);
        final var entity2 = entity("2", TIMESTAMP.plusNanos(1));
        final var entity3 = entity("3", TIMESTAMP.plusMillis(1));

        final var result = entityTimestampsService.updateEntityTimestamps(latest, List.of(entity1, entity2, entity3));

        assertThat(result).containsExactly(entity1, entity2, entity3);
    }

    @Test
    void shouldPushBackEntitiesBeforeLatestPage_whenSomeEntities() {
        final var latest = latestPage(TIMESTAMP);
        final var entity1 = entity("1", TIMESTAMP.plusSeconds(-2));
        final var entity2 = entity("2", TIMESTAMP.plusSeconds(-2));
        final var entity3 = entity("3", TIMESTAMP.plusSeconds(-1));
        final var entity4 = entity("4", TIMESTAMP.plusSeconds(1));
        final var entity5 = entity("5", TIMESTAMP.plusSeconds(2));

        final var result = entityTimestampsService.updateEntityTimestamps(latest, List.of(
            entity1,
            entity2,
            entity3,
            entity4,
            entity5
        ));

        assertThat(result).containsExactly(
            copyEntity(entity1, TIMESTAMP.plusMillis(1), TIMESTAMP.plusSeconds(-2)),
            copyEntity(entity2, TIMESTAMP.plusMillis(1), TIMESTAMP.plusSeconds(-2)),
            copyEntity(entity3, TIMESTAMP.plusMillis(2), TIMESTAMP.plusSeconds(-1)),
            entity4,
            entity5
        );
    }

    @Test
    void shouldPushBackEntitiesBeforeLatestPage_whenAllEntities() {
        final var latest = latestPage(TIMESTAMP);
        final var entity1 = entity("1", TIMESTAMP.plusMillis(-10));
        final var entity2 = entity("2", TIMESTAMP.plusMillis(-5));

        final var result = entityTimestampsService.updateEntityTimestamps(latest, List.of(entity1, entity2));

        assertThat(result).containsExactly(
            copyEntity(entity1, TIMESTAMP.plusMillis(1), TIMESTAMP.plusMillis(-10)),
            copyEntity(entity2, TIMESTAMP.plusMillis(2), TIMESTAMP.plusMillis(-5))
        );
    }

    @Test
    void shouldSuccessivelyPushBackEntitiesAfterLatestPage_toPreserveOrdering() {
        final var latest = latestPage(TIMESTAMP);
        final var entity1 = entity("1", TIMESTAMP.plusMillis(-6000));
        final var entity2 = entity("2", TIMESTAMP.plusMillis(-1000));
        final var entity3 = entity("3", TIMESTAMP.plusMillis(-1000));
        final var entity4 = entity("4", TIMESTAMP);
        final var entity5 = entity("5", TIMESTAMP.plusMillis(1));
        final var entity6 = entity("6", TIMESTAMP.plusMillis(2));
        final var entity7 = entity("7", TIMESTAMP.plusMillis(2));
        final var entity8 = entity("8", TIMESTAMP.plusMillis(5));
        final var entity9 = entity("9", TIMESTAMP.plusMillis(1000));

        final var result = entityTimestampsService.updateEntityTimestamps(latest, List.of(
            entity1,
            entity2,
            entity3,
            entity4,
            entity5,
            entity6,
            entity7,
            entity8,
            entity9
        ));

        assertThat(result).containsExactly(
            copyEntity(entity1, TIMESTAMP.plusMillis(1), TIMESTAMP.plusMillis(-6000)),
            copyEntity(entity2, TIMESTAMP.plusMillis(2), TIMESTAMP.plusMillis(-1000)),
            copyEntity(entity3, TIMESTAMP.plusMillis(2), TIMESTAMP.plusMillis(-1000)),
            copyEntity(entity4, TIMESTAMP.plusMillis(3), TIMESTAMP),
            copyEntity(entity5, TIMESTAMP.plusMillis(4), TIMESTAMP.plusMillis(1)),
            copyEntity(entity6, TIMESTAMP.plusMillis(5), TIMESTAMP.plusMillis(2)),
            copyEntity(entity7, TIMESTAMP.plusMillis(5), TIMESTAMP.plusMillis(2)),
            copyEntity(entity8, TIMESTAMP.plusMillis(6), TIMESTAMP.plusMillis(5)),
            entity9
        );
    }

    private static FeedEntityRepository.PageAssignment entity(String id, Instant lastModified) {
        return new FeedEntityRepository.PageAssignment(
            ContentId.of(id),
            Timestamp.of(lastModified),
            Optional.empty(),
            1,
            Optional.empty()
        );
    }

    private static FeedEntityRepository.PageAssignment copyEntity(
        FeedEntityRepository.PageAssignment entity,
        Instant lastModified,
        Instant originalLastModified
    ) {
        return new FeedEntityRepository.PageAssignment(
            entity.contentId(),
            Timestamp.of(lastModified),
            Optional.of(Timestamp.of(originalLastModified)),
            entity.contentLength(),
            entity.pageId()
        );
    }

    private static FeedPageMetadataRepository.PageMetadata latestPage(Instant lastModified) {
        return new FeedPageMetadataRepository.PageMetadata(
            PageId.of("latest-page"),
            Timestamp.of(lastModified),
            Optional.empty(),
            Optional.empty(),
            1,
            1,
            1
        );
    }
}
