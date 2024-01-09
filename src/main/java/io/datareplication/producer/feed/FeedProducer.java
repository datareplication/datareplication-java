package io.datareplication.producer.feed;

import io.datareplication.model.Body;
import io.datareplication.model.Entity;
import io.datareplication.model.feed.FeedEntityHeader;
import io.datareplication.model.feed.OperationType;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;

import java.time.Clock;
import java.util.concurrent.CompletionStage;

public interface FeedProducer {
    @NonNull CompletionStage<Void> publish(@NonNull OperationType operationType, @NonNull Body body);

    @NonNull CompletionStage<Void> publish(@NonNull OperationType operationType,
                                           @NonNull Body body,
                                           @NonNull Object userData);

    @NonNull CompletionStage<Void> publish(@NonNull Entity<@NonNull FeedEntityHeader> entity);

    /**
     * @return the number of entities added to pages
     */
    // TODO: is there a better name?
    @NonNull CompletionStage<Integer> assignPages();

    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class Builder {
        private final FeedEntityRepository feedEntityRepository;
        private final FeedPageMetadataRepository feedPageMetadataRepository;
        private final FeedProducerJournalRepository feedProducerJournalRepository;
        private Clock clock;
        private int assignPagesLimit;
        // TODO: more settings

        /**
         * Use the given {@link Clock} when generating timestamps for new entities.
         *
         * @param clock the clock to use for timestamps
         * @return this builder
         */
        @NonNull Builder clock(@NonNull Clock clock) {
            this.clock = clock;
            return this;
        }

        @NonNull Builder assignPagesLimit(int limit) {
            if (limit <= 0) {
                throw new IllegalArgumentException("limit must be >= 1");
            }
            this.assignPagesLimit = limit;
            return this;
        }

        @NonNull FeedProducer build() {
            return new FeedProducerImpl(
                feedEntityRepository,
                feedPageMetadataRepository,
                feedProducerJournalRepository,
                clock,
                new RandomContentIdProvider(),
                new RollbackService(
                    feedEntityRepository,
                    feedPageMetadataRepository
                ),
                new EntityTimestampsService(),
                new AssignPagesService(),
                assignPagesLimit
            );
        }
    }

    static @NonNull Builder builder(@NonNull FeedEntityRepository feedEntityRepository,
                                    @NonNull FeedPageMetadataRepository feedPageMetadataRepository,
                                    @NonNull FeedProducerJournalRepository feedProducerJournalRepository) {
        return new Builder(feedEntityRepository,
            feedPageMetadataRepository,
            feedProducerJournalRepository,
            Clock.systemUTC(),
            10000);
    }
}
