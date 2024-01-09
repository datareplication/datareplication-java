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
        private int assignPagesLimit = Integer.MAX_VALUE;
        private long maxBytesPerPage = 1000 * 1000;
        private long maxEntriesPerPage = Long.MAX_VALUE;
        // TODO: more settings

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
         * <p>Set the maximum number of entities to load and assign in a single {@link FeedProducer#assignPages()} call.</p>
         *
         * <p>This setting exists to prevent out-of-memory errors if a large amount of unassigned entities have accumulated.
         * It defaults to {@link Integer#MAX_VALUE}, i.e. in practice unlimited.
         * </p>
         *
         * @param limit the maximum number of entities per call
         * @return this builder
         * @throws IllegalArgumentException if the argument is &lt; 1
         */
        public @NonNull Builder assignPagesLimit(int limit) {
            if (limit <= 0) {
                throw new IllegalArgumentException("limit must be >= 1");
            }
            this.assignPagesLimit = limit;
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
         * Set the maximum entries per page. When a page is composed, a new page will be created if the current page
         * has too many entries. Defaults to Long.MAX_VALUE (meaning no limit).
         *
         * @param maxEntriesPerPage the maximum entries per page. Must be equal or greater than 1.
         * @return this builder
         * @throws IllegalArgumentException if the argument is &lt; 1
         */
        public @NonNull Builder maxEntriesPerPage(final long maxEntriesPerPage) {
            if (maxEntriesPerPage <= 0) {
                throw new IllegalArgumentException("maxEntriesPerPage must be >= 1");
            }
            this.maxEntriesPerPage = maxEntriesPerPage;
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
                new EntityTimestampsService(),
                new AssignPagesService(maxBytesPerPage, maxEntriesPerPage),
                assignPagesLimit
            );
        }
    }

    static @NonNull Builder builder(@NonNull FeedEntityRepository feedEntityRepository,
                                    @NonNull FeedPageMetadataRepository feedPageMetadataRepository,
                                    @NonNull FeedProducerJournalRepository feedProducerJournalRepository) {
        return new Builder(feedEntityRepository, feedPageMetadataRepository, feedProducerJournalRepository);
    }
}
