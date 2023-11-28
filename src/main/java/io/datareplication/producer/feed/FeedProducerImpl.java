package io.datareplication.producer.feed;

import io.datareplication.model.Body;
import io.datareplication.model.Entity;
import io.datareplication.model.Timestamp;
import io.datareplication.model.feed.FeedEntityHeader;
import io.datareplication.model.feed.OperationType;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;

import java.time.Clock;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

@AllArgsConstructor(access = AccessLevel.PACKAGE)
class FeedProducerImpl implements FeedProducer {
    private final FeedEntityRepository feedEntityRepository;
    private final FeedPageMetadataRepository feedPageMetadataRepository;
    private final FeedProducerJournalRepository feedProducerJournalRepository;
    private final Clock clock;
    private final RandomContentIdProvider contentIdProvider;
    private final int assignPagesLimit;

    @Override
    public @NonNull CompletionStage<Void> publish(@NonNull final OperationType operationType,
                                                  @NonNull final Body body) {
        return publish(operationType, body, Optional.empty());
    }

    @Override
    public @NonNull CompletionStage<Void> publish(@NonNull final OperationType operationType,
                                                  @NonNull final Body body,
                                                  @NonNull final Object userData) {
        return publish(operationType, body, Optional.of(userData));
    }

    private CompletionStage<Void> publish(OperationType operationType, Body body, Optional<Object> userData) {
        final var header = new FeedEntityHeader(Timestamp.of(clock.instant()),
                                                      operationType,
                                                      contentIdProvider.newContentId());
        final var entity = new Entity<>(header, body, userData);
        return publish(entity);
    }

    @Override
    public @NonNull CompletionStage<Void> publish(@NonNull final Entity<@NonNull FeedEntityHeader> entity) {
        return feedEntityRepository.append(entity);
    }

    // TODO: Let's talk about assignPagesLimit. Should this be a limit per call (i.e. every time you call assignPages, it
    //  will process at most limit entities, i.e. it will never return a number > the limit? Or is it a batch size, i.e.
    //  assignPages will always process as much as it can find, but it will split the work into blocks of at most limit
    //  entities to prevent OOM? In this case the return value might well be > limit. But it might also cause assignPages
    //  to effectively loop for an indeterminate amount of time if there's a constant drip of entities.
    //  I'm leaning towards hard limit, because you can easily build the loop on top, but you can't easily get
    //  guaranteed-limited work units from the loop version.
    @Override
    public @NonNull CompletionStage<Integer> assignPages() {
        throw new UnsupportedOperationException("not implemented");
    }
}
