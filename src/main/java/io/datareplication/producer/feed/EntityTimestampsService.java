package io.datareplication.producer.feed;

import io.datareplication.model.Timestamp;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

class EntityTimestampsService {
    /**
     * Duration that entities are moved into the future.
     */
    private static final Duration BUMP = Duration.ofMillis(1);

    /**
     * Update entity timestamps to ensure that all entities that we're about to assign have timestamps at least as new
     * as the current latest page. The current ordering of entities is preserved as far as possible, meaning entities are
     * incrementally bumped to avoid any entity being skipped past an originally later entity.
     *
     * @param latestPage the current latest page
     * @param entities   entities to update
     * @return all entities that were passed in, with timestamps updated where necessary
     */
    List<FeedEntityRepository.PageAssignment> updateEntityTimestamps(
        FeedPageMetadataRepository.PageMetadata latestPage,
        List<FeedEntityRepository.PageAssignment> entities
    ) {
        class State {
            Timestamp prev = latestPage.lastModified();
            Timestamp prevBeforeUpdate = latestPage.lastModified();
        }
        final var state = new State();

        return entities
            .stream()
            .map(entity -> {
                final var updatedTimestamp = updatedTimestamp(entity.lastModified(), state.prev, state.prevBeforeUpdate);
                state.prev = updatedTimestamp;
                state.prevBeforeUpdate = entity.lastModified();
                return updatedEntity(entity, updatedTimestamp);
            })
            .collect(Collectors.toList());

    }

    private Timestamp updatedTimestamp(Timestamp current, Timestamp prev, Timestamp prevBeforeUpdate) {
        if (current.value().isAfter(prev.value())) {
            // If the current timestamp is after the previous timestamp, it's already good and we keep it.
            return current;
        } else {
            if (current.equals(prevBeforeUpdate)) {
                // If the current entity's timestamp is the same as the previous entity's timestamp before it was updated,
                // we use the previous entity's timestamp, i.e. entities that shared a timestamp before updating have the
                // same updated timestamp.
                return prev;
            } else {
                // Otherwise we set the current entity's timestamp to slightly after the previous timestamp.
                return Timestamp.of(prev.value().plus(BUMP));
            }
        }
    }

    private static FeedEntityRepository.PageAssignment updatedEntity(FeedEntityRepository.PageAssignment entity, Timestamp lastModified) {
        if (entity.lastModified().equals(lastModified)) {
            return entity;
        } else {
            return new FeedEntityRepository.PageAssignment(
                entity.contentId(),
                lastModified,
                Optional.of(entity.lastModified()),
                entity.contentLength(),
                entity.pageId()
            );
        }
    }
}
