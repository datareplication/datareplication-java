package io.datareplication.producer.feed;

import io.datareplication.model.PageId;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class GenerationsTest {
    @Test
    void shouldReturnNothing_ifNoCandidates() {
        final var result = Generations.selectLatestPage(List.of());

        assertThat(result).isEmpty();
    }

    @Test
    void shouldReturnSingleCandidate() {
        final var page1 = page(4);

        final var result = Generations.selectLatestPage(List.of(page1));

        assertThat(result).contains(page1);
    }

    @Test
    void shouldReturnCandidateWithLowestGeneration() {
        final var page1 = page(3);
        final var page2 = page(5);
        final var page3 = page(1);
        final var page4 = page(2);

        final var result = Generations.selectLatestPage(List.of(page1, page2, page3, page4));

        assertThat(result).contains(page3);
    }

    private static FeedPageMetadataRepository.PageMetadata page(int generation) {
        return new FeedPageMetadataRepository.PageMetadata(
            PageId.of("a-page"),
            Instant.now(),
            Optional.empty(),
            Optional.empty(),
            1,
            1,
            generation
        );
    }
}
