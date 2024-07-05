package io.datareplication.producer.feed;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * This description has to go somewhere, so it goes here: the "generation" as stored in the page repo via
 * {@link FeedPageMetadataRepository.PageMetadata#generation()} is a little field with one very specific role: we need
 * to be able to determine which page is the "latest" page, even for the very short timeframe when we have two pages
 * that qualify.
 * <p>
 * The "latest" page, both for serving pages and for figuring out where to continue, is always the page in the repo that
 * has no "next" link. This is convenient, doesn't need another repository and allows an atomic switchover to a newly
 * created "latest". However, when saving pages during {@link FeedProducerImpl#assignPages()}, there's a brief moment
 * after we save the new next-link-less "latest" page where we have two pages without next link. During that time, we
 * want to still serve the old "latest" page as the latest page, but we have no way to determine which one that is (in
 * 100% of real-world situations, rounded up, you can look at the timestamp and pick the one with the older timestamp,
 * but in theory the timestamps may be the same down to the nanosecond so that method is not edge-case-proof). To solve
 * this issue, we save every page with a generation number that's incremented for each batch of pages created. At any
 * given time, the correct "latest" page is the one with the lowest generation.
 * <p>
 * To avoid rollover, we check the generation on the "latest" page before generating a batch of pages, and if it's
 * reached a limit (see {@link #MAX_GENERATION}) we reset it and save the page during a time when we only have one page
 * without next link (see {@link GenerationRotationService#rotateGenerationIfNecessary(Optional)}).
 * <p>
 * Technically this could be implemented with as few as three different generation values arranged in a cycle:
 * <pre>a < b < c < a < b < ...</pre>
 * However, a 32-bit int is very straightforward to store in a database and is less
 * heavyweight, conceptually, than a custom enum.
 */
final class Generations {
    static final int INITIAL_GENERATION = 0;
    static final int MAX_GENERATION = 1_000_000_000;

    private Generations() {
    }

    /**
     * Pick the correct latest page from a list of candidates (pages with no next link) based on the generation.
     *
     * @param candidates candidate pages
     * @return the "correct" latest page
     */
    static Optional<FeedPageMetadataRepository.PageMetadata> selectLatestPage(
        List<FeedPageMetadataRepository.PageMetadata> candidates
    ) {
        // TODO: filter any candidate that actually does have a next link (see FeedPageMetadataRepository and my
        //  fretting about index consistency)
        final var generationComparator = Comparator.comparingInt(FeedPageMetadataRepository.PageMetadata::generation);
        return candidates.stream().min(generationComparator);
    }
}
