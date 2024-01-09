package io.datareplication.producer.feed;

import io.datareplication.model.PageId;
import io.datareplication.model.Timestamp;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FeedPageMetadataRepositoryActionsTest {
    private final FeedPageMetadataRepository repository = mock(FeedPageMetadataRepository.class);

    @Test
    void shouldReturnEmpty_whenNoPagesWithoutNextLink() {
        when(repository.getWithoutNextLink())
            .thenReturn(CompletableFuture.supplyAsync(Collections::emptyList));

        final var result = FeedPageMetadataRepositoryActions
            .getLatest(repository)
            .block();

        assertThat(result).isEmpty();
    }

    @Test
    void shouldReturnPageWithLowestGeneration() {
        final var page1 = somePage("1", 3);
        final var page2 = somePage("2", 5);
        final var page3 = somePage("3", 1);
        final var page4 = somePage("4", 2);
        when(repository.getWithoutNextLink())
            .thenReturn(CompletableFuture.supplyAsync(() -> List.of(page1, page2, page3, page4)));

        final var result = FeedPageMetadataRepositoryActions
            .getLatest(repository)
            .block();

        assertThat(result).contains(page3);
    }

    private static FeedPageMetadataRepository.PageMetadata somePage(String id, int generation) {
        return new FeedPageMetadataRepository.PageMetadata(
            PageId.of(id),
            Timestamp.now(),
            Optional.empty(),
            Optional.empty(),
            1,
            1,
            generation
        );
    }
}
