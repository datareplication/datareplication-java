package io.datareplication.producer.feed;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import reactor.core.publisher.Mono;

@AllArgsConstructor(access = AccessLevel.PACKAGE)
class RollbackService {
    private final FeedEntityRepository feedEntityRepository;
    private final FeedPageMetadataRepository feedPageMetadataRepository;

    Mono<Void> rollback(FeedProducerJournalRepository.JournalState journalState) {
        throw new RuntimeException("not implemented");
    }
}
