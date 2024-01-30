package io.datareplication.producer.feed;

import io.datareplication.model.PageId;
import io.datareplication.model.Timestamp;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FeedPageProviderImplTest {
    private final FeedEntityRepository feedEntityRepository = mock(FeedEntityRepository.class);
    private final FeedPageMetadataRepository feedPageMetadataRepository = mock(FeedPageMetadataRepository.class);
    private final FeedPageProvider feedPageProvider = new FeedPageProviderImpl(
        feedEntityRepository,
        feedPageMetadataRepository
    );

    @Test
    void latestPageId_shouldReturnNoPageId_whenNoPagesWithoutNextLink() {
        when(feedPageMetadataRepository.getWithoutNextLink())
            .thenReturn(Mono.just(Collections.<FeedPageMetadataRepository.PageMetadata>emptyList()).toFuture());

        var result = feedPageProvider.latestPageId();

        StepVerifier
            .create(Mono.fromCompletionStage(result))
            .expectNext(Optional.empty())
            .expectComplete()
            .verify();
    }

    @Test
    void latestPageId_shouldReturnPageId_whenExactlyOnePageWithoutNextLink() {
        when(feedPageMetadataRepository.getWithoutNextLink())
            .thenReturn(Mono.just(List.of(page("page1", 4))).toFuture());

        var result = feedPageProvider.latestPageId();

        StepVerifier
            .create(Mono.fromCompletionStage(result))
            .expectNext(Optional.of(PageId.of("page1")))
            .expectComplete()
            .verify();
    }

    @Test
    void latestPageId_shouldReturnPageIdWithLowestGeneration_whenMultiplePagesWithoutNextLink() {
        when(feedPageMetadataRepository.getWithoutNextLink()).thenReturn(Mono.just(List.of(
            page("page1", 4),
            page("page2", 3),
            page("page3", 6)
        )).toFuture());

        var result = feedPageProvider.latestPageId();

        StepVerifier
            .create(Mono.fromCompletionStage(result))
            .expectNext(Optional.of(PageId.of("page2")))
            .expectComplete()
            .verify();
    }

    private FeedPageMetadataRepository.PageMetadata page(String pageId, int generation) {
        return new FeedPageMetadataRepository.PageMetadata(
            PageId.of(pageId),
            Timestamp.of(Instant.parse("2024-01-30T16:24:00Z")),
            Optional.empty(),
            Optional.empty(),
            15,
            1,
            generation
        );
    }
}
