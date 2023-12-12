package io.datareplication.producer.feed;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import reactor.core.publisher.Mono;

@AllArgsConstructor(access = AccessLevel.PACKAGE)
class RollbackService {
    private final FeedEntityRepository feedEntityRepository;
    private final FeedPageMetadataRepository feedPageMetadataRepository;

    Mono<Void> rollback(FeedProducerJournalRepository.JournalState journalState) {
        // notes:
        // TODO: journal probably should save differently (old latest page separate from the others) for easier rollback
        //  because there's only two possibilities:
        //  - latest page switched -> everything clean because things are visible
        //  - old latest page -> switch didn't happen, delete all new pages and clean up non-visible entities for old (still current) latest page
        //                       or maybe: there were no new pages and the page entry was or was not updated, but we still just clean up
        //                       all non-visible entities because if the page entry was updated they'd be visible
        //  clean up entity = unset page ID, copy originalLastModified to lastModified if set and unset originalLastModified

        throw new RuntimeException("not implemented");
    }
}
