package io.datareplication.producer.feed;

import io.datareplication.model.Body;
import io.datareplication.model.Entity;
import io.datareplication.model.PageId;
import io.datareplication.model.feed.ContentId;
import io.datareplication.model.feed.FeedEntityHeader;
import lombok.NonNull;
import lombok.Value;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

/**
 * Repository to store feed entities and what page they are assigned to.
 * <p>
 * Entities are uniquely identified by their {@link ContentId}. Different methods alternatively take and return
 * either full entities ({@link Entity}) or a subset of header fields ({@link PageAssignment}). All these methods
 * act on the same data, they just provide different views onto the same records, to avoid having to load full
 * entity bodies when they're not needed. The fields that need to be stored in this repository is the union of
 * all fields in both classes.  See the documentation for {@link PageAssignment} on how to match the
 * fields.
 * <p>
 * As an exception to the rule above, the following fields don't need to be accurately saved:
 * <ul>
 *     <li>{@link FeedEntityHeader#extraHeaders()} may be ignored if you don't intend to use it</li>
 *     <li>{@link Entity#userData()} explicitly does not have any meaning ascribed to it by the library;
 *         consequently, it is entirely up to the repository implementation how to handle this field</li>
 * </ul>
 *
 * <h2>Consistency Requirements</h2>
 * This repository must fulfill
 * {@link io.datareplication.producer.feed the common requirements for all feed producer repositories}.
 * <p>
 * All write operations must be atomic for an individual entity, i.e. it must never be possible to observe a
 * partially-written entity
 *
 * <h2>Ordering</h2>
 * Any returned lists of entities must be sorted in the following way:
 * <ol>
 *     <li>by their last-modified timestamp ({@link PageAssignment#lastModified()}), oldest to newest</li>
 *     <li>any entities with the same timestamp must be sorted using their content ID as a tie breaker</li>
 * </ol>
 * It's important that the ordering for any two entities remains stable so long as their last-modified timestamps
 * don't change.
 */
public interface FeedEntityRepository {
    /**
     * A subset of this repository's fields that are sufficient for operations that don't need the entity body.
     * <p>
     * Some of the fields in this class correspond to fields in {@link Entity} or {@link FeedEntityHeader}. See
     * the field documentation for references.
     */
    // TODO: split in two (output and update) to not make it seem like contentLength (and what else?) can ever change?
    @Value
    class PageAssignment {
        /**
         * The entity's content ID. Corresponds to {@link FeedEntityHeader#contentId()}.
         */
        @NonNull
        ContentId contentId;
        /**
         * The entity's last-modified timestamp. Corresponds to {@link FeedEntityHeader#lastModified()}.
         */
        @NonNull
        Instant lastModified;
        /**
         * The old last-modified timestamp in case the entity is pushed backwards. Does not correspond to a field
         * in {@link Entity}. When an entity is initially saved into the repository, this field should be stored
         * empty.
         */
        @NonNull
        Optional<@NonNull Instant> originalLastModified;
        /**
         * The entity's body length. Corresponds to {@link Body#contentLength()}.
         */
        long contentLength;
        /**
         * The ID of the page that the entity is assigned to. Does not correspond to a field in {@link Entity}. When
         * an entity is initially saved into the repository, this field should be stored empty.
         */
        @NonNull
        Optional<@NonNull PageId> pageId;
    }

    /**
     * Add an entity to the repository. Once an entity has been saved successfully,
     * this method won't be called again with the same content ID.
     * <p>
     * The {@link Entity} object does not contain all fields that need to be saved in the repository. See
     * {@link PageAssignment} for the expected default values for any fields missing from the entity.
     *
     * @param entity the entity to save
     * @return CompletionStage
     */
    @NonNull
    CompletionStage<Void> append(@NonNull Entity<@NonNull FeedEntityHeader> entity);

    /**
     * Load all entities whose {@link PageAssignment#pageId()} field is set to the given page.
     * <p>
     * All fields necessary to create the {@link Entity} must be loaded, including the body. The returned list must
     * be sorted by the entities' timestamps as described in the class documentation.
     *
     * @param pageId the page ID to load
     * @return CompletionStage of all entities assigned to the given page
     */
    @NonNull
    CompletionStage<@NonNull List<@NonNull Entity<@NonNull FeedEntityHeader>>> get(@NonNull PageId pageId);

    /**
     * Load all entities whose {@link PageAssignment#pageId()} field is empty.
     * <p>
     * The returned list must be sorted by the entities' timestamps as described in the class documentation. Note
     * that the limit must take place after sorting, i.e. logically the list of all available records is sorted
     * and then the first <pre>limit</pre> elements from the sorted list are returned.
     *
     * @param limit the maximum number of records to load
     * @return CompletionStage of all entities not assigned to a page
     */
    @NonNull
    CompletionStage<@NonNull List<@NonNull PageAssignment>> getUnassigned(int limit);

    /**
     * Load all entities whose {@link PageAssignment#pageId()} field is set to the given page.
     * <p>
     * The returned list must be sorted by the entities' timestamps as described in the class documentation.
     *
     * @param pageId the page ID to load
     * @return CompletionStage of all entities assigned to the given page
     */
    @NonNull
    CompletionStage<@NonNull List<@NonNull PageAssignment>> getPageAssignments(@NonNull PageId pageId);

    /**
     * Update the given entities.
     * <p>
     * This method will only ever update existing entities, i.e. all content IDs in the list are guaranteed to
     * exist in the repository.
     *
     * @param pageAssignments a list of updates to save to the repository
     * @return CompletionStage
     */
    @NonNull
    CompletionStage<Void> savePageAssignments(@NonNull List<@NonNull PageAssignment> pageAssignments);
}
