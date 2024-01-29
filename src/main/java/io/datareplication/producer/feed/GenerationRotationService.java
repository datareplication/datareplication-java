package io.datareplication.producer.feed;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;

@AllArgsConstructor(access = AccessLevel.PACKAGE)
class GenerationRotationService {
    final FeedPageMetadataRepository feedPageMetadataRepository;

    /**
     * If the page's generation is too large, we reset it and save that.
     * @param maybeLatestPage the current latest page, maybe
     * @return the effective latest page: the updated one if we rotated generations or the original one otherwise
     */
    Mono<Optional<FeedPageMetadataRepository.PageMetadata>> rotateGenerationIfNecessary(Optional<FeedPageMetadataRepository.PageMetadata> maybeLatestPage) {
        return maybeLatestPage
            .filter(latestPage -> latestPage.generation() >= Generations.MAX_GENERATION)
            .map(this::rotateGeneration)
            .orElse(Mono.just(maybeLatestPage));
    }

    private Mono<Optional<FeedPageMetadataRepository.PageMetadata>> rotateGeneration(FeedPageMetadataRepository.PageMetadata latestPage) {
        final var newLatestPage = new FeedPageMetadataRepository.PageMetadata(
            latestPage.pageId(),
            latestPage.lastModified(),
            latestPage.prev(),
            latestPage.next(),
            latestPage.numberOfBytes(),
            latestPage.numberOfEntities(),
            Generations.INITIAL_GENERATION
        );
        return Mono
            .fromCompletionStage(() -> feedPageMetadataRepository.save(List.of(newLatestPage)))
            .thenReturn(Optional.of(newLatestPage));
    }
}
