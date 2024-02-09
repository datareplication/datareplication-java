package io.datareplication.producer.feed.testhelper;

import io.datareplication.model.Entity;
import io.datareplication.model.PageId;
import io.datareplication.model.feed.ContentId;
import io.datareplication.model.feed.FeedEntityHeader;
import io.datareplication.producer.feed.FeedEntityRepository;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class FeedEntityFaultRepository implements FeedEntityRepository {

    private final FeedEntityRepository inner;
    private final HashSet<ContentId> fail = new HashSet<>();

    public void failOn(ContentId... contentIds) {
        fail.addAll(Arrays.asList(contentIds));
    }

    public void succeed() {
        fail.clear();
    }

    @Override
    public @NonNull CompletionStage<Void> append(@NonNull Entity<@NonNull FeedEntityHeader> entity) {
        return inner.append(entity);
    }

    @Override
    public @NonNull CompletionStage<@NonNull List<@NonNull Entity<@NonNull FeedEntityHeader>>> get(@NonNull PageId pageId) {
        return inner.get(pageId);
    }

    @Override
    public @NonNull CompletionStage<@NonNull List<@NonNull PageAssignment>> getUnassigned(int limit) {
        return inner.getUnassigned(limit);
    }

    @Override
    public @NonNull CompletionStage<@NonNull List<@NonNull PageAssignment>> getPageAssignments(@NonNull PageId pageId) {
        return inner.getPageAssignments(pageId);
    }

    @Override
    public @NonNull CompletionStage<Void> savePageAssignments(@NonNull List<@NonNull PageAssignment> pageAssignments) {
        var successful = pageAssignments
            .stream()
            .takeWhile(p -> !fail.contains(p.contentId()))
            .collect(Collectors.toList());
        return inner
            .savePageAssignments(successful)
            .thenAccept(v -> {
                if (successful.size() < pageAssignments.size()) {
                    throw new FaultRepositoryException();
                }
            });
    }
}
