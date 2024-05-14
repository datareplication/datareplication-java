package io.datareplication.producer.feed;

import io.datareplication.model.Body;
import io.datareplication.model.Entity;
import io.datareplication.model.feed.FeedEntityHeader;
import io.datareplication.model.feed.OperationType;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

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

    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    class Builder {
        private final FeedEntityRepository feedEntityRepository;
        private final FeedPageMetadataRepository feedPageMetadataRepository;
        private final FeedProducerJournalRepository feedProducerJournalRepository;
        private Clock clock = Clock.systemUTC();
        private int assignPagesLimitPerRun = Integer.MAX_VALUE;
        private long maxBytesPerPage = 1000 * 1000;
        private long maxEntitiesPerPage = Long.MAX_VALUE;

        /**
         * Use the given {@link Clock} when generating timestamps for new entities.
         *
         * @param clock the clock to use for timestamps
         * @return this builder
         */
        public @NonNull Builder clock(@NonNull Clock clock) {
            this.clock = clock;
            return this;
        }

        /**
         * Set the maximum number of entities to load and assign in a single {@link FeedProducer#assignPages()} call.
         * <p>
         * This setting exists to prevent out-of-memory errors if a large amount of unassigned entities have
         * accumulated. It defaults to {@link Integer#MAX_VALUE}, i.e. in practice unlimited.
         *
         * @param limit the maximum number of entities to load and assign per run
         * @return this builder
         * @throws IllegalArgumentException if the argument is &lt; 1
         */
        public @NonNull Builder assignPagesLimitPerRun(int limit) {
            if (limit <= 0) {
                throw new IllegalArgumentException("limit must be >= 1");
            }
            this.assignPagesLimitPerRun = limit;
            return this;
        }

        /**
         * Set the maximum bytes per page. When a page is composed, a new page will be created if the current page
         * gets too big. Defaults to 1 MB.
         *
         * @param maxBytesPerPage the maximum bytes per page. Must be equal or greater than 1.
         * @return this builder
         * @throws IllegalArgumentException if the argument is &lt; 1
         */
        public @NonNull Builder maxBytesPerPage(final long maxBytesPerPage) {
            if (maxBytesPerPage <= 0) {
                throw new IllegalArgumentException("maxBytesPerPage must be >= 1");
            }
            this.maxBytesPerPage = maxBytesPerPage;
            return this;
        }

        /**
         * Set the maximum entities per page. When a page is composed, a new page will be created if the current page
         * has too many entities. Defaults to Long.MAX_VALUE (meaning no limit).
         *
         * @param maxEntitiesPerPage the maximum number of entities per page. Must be equal or greater than 1.
         * @return this builder
         * @throws IllegalArgumentException if the argument is &lt; 1
         */
        public @NonNull Builder maxEntitiesPerPage(final long maxEntitiesPerPage) {
            if (maxEntitiesPerPage <= 0) {
                throw new IllegalArgumentException("maxEntitiesPerPage must be >= 1");
            }
            this.maxEntitiesPerPage = maxEntitiesPerPage;
            return this;
        }

        public @NonNull FeedProducer build() {
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
                new GenerationRotationService(feedPageMetadataRepository),
                new EntityTimestampsService(),
                new AssignPagesService(new RandomPageIdProvider(), maxBytesPerPage, maxEntitiesPerPage),
                assignPagesLimitPerRun
            );
        }
    }

    static @NonNull Builder builder(@NonNull FeedEntityRepository feedEntityRepository,
                                    @NonNull FeedPageMetadataRepository feedPageMetadataRepository,
                                    @NonNull FeedProducerJournalRepository feedProducerJournalRepository) {
        return new Builder(feedEntityRepository, feedPageMetadataRepository, feedProducerJournalRepository);
    }
}
