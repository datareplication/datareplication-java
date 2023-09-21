package io.datareplication.producer.feed;

import io.datareplication.model.Body;
import io.datareplication.model.Entity;
import io.datareplication.model.feed.FeedEntityHeader;
import io.datareplication.model.feed.OperationType;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;

import java.time.Clock;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

public interface FeedProducer {
    @NonNull CompletionStage<Void> publish(@NonNull OperationType operationType, @NonNull Body body);

    @NonNull CompletionStage<Void> publish(@NonNull OperationType operationType,
                                           @NonNull Body body,
                                           @NonNull Object userData);

    @NonNull CompletionStage<Void> publish(@NonNull OperationType operationType,
                                           @NonNull Body body,
                                           @NonNull Optional<@NonNull Object> userData);

    @NonNull CompletionStage<Void> publish(@NonNull Entity<@NonNull FeedEntityHeader> entity);

    @NonNull CompletionStage<Void> assignPages();

    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class Builder {
        private FeedEntityRepository feedEntityRepository;
        private FeedPageMetadataRepository feedPageMetadataRepository;
        private FeedProducerJournalRepository feedProducerJournalRepository;
        private FeedPageUrlBuilder feedPageUrlBuilder;
        private Clock clock;
        // TODO: threadpool
        // TODO: more settings

        @NonNull Builder clock(@NonNull Clock clock) {
            this.clock = clock;
            return this;
        }

        @NonNull FeedProducer build() {
            throw new RuntimeException("not implemented");
        }
    }

    static @NonNull Builder builder(@NonNull FeedEntityRepository feedEntityRepository,
                                    @NonNull FeedPageMetadataRepository feedPageMetadataRepository,
                                    @NonNull FeedProducerJournalRepository feedProducerJournalRepository,
                                    @NonNull FeedPageUrlBuilder feedPageUrlBuilder) {
        return new Builder(feedEntityRepository,
                           feedPageMetadataRepository,
                           feedProducerJournalRepository,
                           feedPageUrlBuilder,
                           Clock.systemUTC());
    }
}
