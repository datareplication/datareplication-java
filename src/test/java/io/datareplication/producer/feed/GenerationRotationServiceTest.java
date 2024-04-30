package io.datareplication.producer.feed;

import io.datareplication.model.PageId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class GenerationRotationServiceTest {
    private final FeedPageMetadataRepository feedPageMetadataRepository = mock(FeedPageMetadataRepository.class);
    private final GenerationRotationService generationRotationService =
        new GenerationRotationService(feedPageMetadataRepository);

    @BeforeEach
    void setUp() {
        when(feedPageMetadataRepository.save(any())).thenReturn(Mono.<Void>empty().toFuture());
    }

    @Test
    void shouldDoNothing_whenNoLatestPage() {
        final var result = generationRotationService
            .rotateGenerationIfNecessary(Optional.empty())
            .block();

        assertThat(result).isEmpty();
        verifyNoInteractions(feedPageMetadataRepository);
    }

    @Test
    void shouldDoNothing_whenGenerationZero() {
        final var page = page(0);

        final var result = generationRotationService
            .rotateGenerationIfNecessary(Optional.of(page))
            .block();

        assertThat(result).contains(page);
        verifyNoInteractions(feedPageMetadataRepository);
    }

    @Test
    void shouldDoNothing_whenGenerationLow() {
        final var page = page(666);

        final var result = generationRotationService
            .rotateGenerationIfNecessary(Optional.of(page))
            .block();

        assertThat(result).contains(page);
        verifyNoInteractions(feedPageMetadataRepository);
    }

    @Test
    void shouldDoNothing_whenGenerationJustUnderMaxGeneration() {
        final var page = page(Generations.MAX_GENERATION - 1);

        final var result = generationRotationService
            .rotateGenerationIfNecessary(Optional.of(page))
            .block();

        assertThat(result).contains(page);
        verifyNoInteractions(feedPageMetadataRepository);
    }

    @Test
    void shouldRotateGenerationAndSave_whenGenerationExactlyMaximumGeneration() {
        final var page = page(Generations.MAX_GENERATION);

        final var result = generationRotationService
            .rotateGenerationIfNecessary(Optional.of(page))
            .block();

        final var expectedPage = copyOf(page, Generations.INITIAL_GENERATION);
        assertThat(result).contains(expectedPage);
        verify(feedPageMetadataRepository).save(List.of(expectedPage));
    }

    @Test
    void shouldRotateGenerationAndSave_whenGenerationOverMaximumGeneration() {
        final var page = page(Generations.MAX_GENERATION + 666666);

        final var result = generationRotationService
            .rotateGenerationIfNecessary(Optional.of(page))
            .block();

        final var expectedPage = copyOf(page, Generations.INITIAL_GENERATION);
        assertThat(result).contains(expectedPage);
        verify(feedPageMetadataRepository).save(List.of(expectedPage));
    }

    @Test
    void shouldRotateGenerationAndSave_whenGenerationIntegerMaxValue() {
        final var page = page(Integer.MAX_VALUE);

        final var result = generationRotationService
            .rotateGenerationIfNecessary(Optional.of(page))
            .block();

        final var expectedPage = copyOf(page, Generations.INITIAL_GENERATION);
        assertThat(result).contains(expectedPage);
        verify(feedPageMetadataRepository).save(List.of(expectedPage));
    }

    private static FeedPageMetadataRepository.PageMetadata page(int generation) {
        return new FeedPageMetadataRepository.PageMetadata(
            PageId.of("latest-page"),
            Instant.now(),
            Optional.empty(),
            Optional.empty(),
            1,
            1,
            generation
        );
    }

    private static FeedPageMetadataRepository.PageMetadata copyOf(
        FeedPageMetadataRepository.PageMetadata page,
        int generation
    ) {
        return new FeedPageMetadataRepository.PageMetadata(
            page.pageId(),
            page.lastModified(),
            page.prev(),
            page.next(),
            page.numberOfBytes(),
            page.numberOfEntities(),
            generation
        );
    }
}
