package io.datareplication.producer.feed.testhelper;

import io.datareplication.model.Entity;
import io.datareplication.model.PageId;
import io.datareplication.model.Timestamp;
import io.datareplication.model.feed.ContentId;
import io.datareplication.model.feed.FeedEntityHeader;
import io.datareplication.producer.feed.FeedEntityRepository;
import lombok.NonNull;
import lombok.Value;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

public final class FeedEntityInMemoryRepository implements FeedEntityRepository {
    private final Map<ContentId, FeedEntityRecord> contents = new HashMap<>();

    @Value
    public static class FeedEntityRecord implements Comparable<FeedEntityRecord> {
        Entity<FeedEntityHeader> entity;
        Optional<PageId> page;
        Optional<Timestamp> originalLastModified;

        private FeedEntityRecord updated(PageAssignment pageAssignment) {
            return new FeedEntityRecord(
                    new Entity<>(
                            new FeedEntityHeader(
                                    pageAssignment.lastModified(),
                                    entity.header().operationType(),
                                    contentId(),
                                    entity.header().extraHeaders()
                            ),
                            entity.body()
                    ),
                    pageAssignment.pageId(),
                    pageAssignment.originalLastModified()
            );
        }

        private ContentId contentId() {
            return entity.header().contentId();
        }

        private PageAssignment pageAssignment() {
            return new PageAssignment(
                    contentId(),
                    entity.header().lastModified(),
                    originalLastModified,
                    (int) entity.body().contentLength(),
                    page
            );
        }

        @Override
        public int compareTo(FeedEntityRecord other) {
            var c1 = this
                    .entity
                    .header()
                    .lastModified()
                    .value()
                    .compareTo(other.entity.header().lastModified().value());
            if (c1 != 0) { //NOPMD
                return c1;
            } else {
                return this
                        .entity
                        .header()
                        .contentId()
                        .value()
                        .compareTo(other.entity.header().contentId().value());
            }
        }
    }

    @Override
    public synchronized @NonNull CompletionStage<Void> append(@NonNull Entity<@NonNull FeedEntityHeader> entity) {
        var record = new FeedEntityRecord(entity, Optional.empty(), Optional.empty());
        synchronized (this) {
            contents.put(record.contentId(), record);
        }
        return CompletableFuture.supplyAsync(() -> null);
    }

    @Override
    public synchronized
    @NonNull CompletionStage<@NonNull List<@NonNull Entity<@NonNull FeedEntityHeader>>>
    get(@NonNull PageId pageId) {
        var result = contents
                .values()
                .stream()
                .filter(r -> r.page.stream().anyMatch(pageId::equals))
                .sorted()
                .map(r -> r.entity)
                .collect(Collectors.toList());
        return CompletableFuture.supplyAsync(() -> result);
    }

    @Override
    public synchronized @NonNull CompletionStage<@NonNull List<@NonNull PageAssignment>> getUnassigned(int limit) {
        var result = contents
                .values()
                .stream()
                .filter(r -> r.page.isEmpty())
                .sorted()
                .limit(limit)
                .map(FeedEntityRecord::pageAssignment)
                .collect(Collectors.toList());
        return CompletableFuture.supplyAsync(() -> result);
    }

    @Override
    public synchronized
    @NonNull CompletionStage<@NonNull List<@NonNull PageAssignment>>
    getPageAssignments(@NonNull PageId pageId) {
        var result = contents
                .values()
                .stream()
                .filter(r -> r.page.stream().anyMatch(pageId::equals))
                .sorted()
                .map(FeedEntityRecord::pageAssignment)
                .collect(Collectors.toList());
        return CompletableFuture.supplyAsync(() -> result);
    }

    @Override
    public synchronized
    @NonNull CompletionStage<Void>
    savePageAssignments(@NonNull List<@NonNull PageAssignment> pageAssignments) {
        for (var assignment : pageAssignments) {
            contents.compute(assignment.contentId(), (a, record) -> {
                if (record == null) {
                    throw new IllegalStateException(
                            "savePageAssignments should never be called for an entity that wasn't saved beforehand"
                    );
                }
                return record.updated(assignment);
            });
        }
        return CompletableFuture.supplyAsync(() -> null);
    }

    public synchronized List<FeedEntityRecord> getAll() {
        return List.copyOf(contents.values());
    }
}
