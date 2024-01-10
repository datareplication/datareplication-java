package io.datareplication.producer.feed;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

class Generations {
    private Generations() {}

    static final int INITIAL_GENERATION = 0;
    static final int MAX_GENERATION = 1000000000;

    /**
     * Pick the correct latest page from a list of candidates (pages with no next link) based on the generation.
     * @param candidates candidate pages
     * @return the "correct" latest page
     */
    static Optional<FeedPageMetadataRepository.PageMetadata> selectLatestPage(List<FeedPageMetadataRepository.PageMetadata> candidates) {
        final var generationComparator = Comparator.comparingInt(FeedPageMetadataRepository.PageMetadata::generation);
        return candidates.stream().min(generationComparator);
    }
}
