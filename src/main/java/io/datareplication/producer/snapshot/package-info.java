/**
 * The snapshot producer creates snapshot pages and the matching snapshot index from a stream of entities and
 * saves them in user-provided repositories.
 *
 * <h2>Usage</h2>
 * The main interface is {@link io.datareplication.producer.snapshot.SnapshotProducer}. To create an instance of it,
 * you need to provide your own implementations of the following interfaces:
 *
 * <ul>
 *     <li>{@link io.datareplication.producer.snapshot.SnapshotPageUrlBuilder} to build the public HTTP
 *         URLs for pages</li>
 *     <li>{@link io.datareplication.producer.snapshot.SnapshotPageRepository} to save generated pages</li>
 *     <li>{@link io.datareplication.producer.snapshot.SnapshotIndexRepository} to save the snapshot index</li>
 * </ul>
 *
 * Then call {@link io.datareplication.producer.snapshot.SnapshotProducer#produce(java.util.concurrent.Flow.Publisher)}
 * with a {@link java.util.concurrent.Flow.Publisher} of entities to create a snapshot.
 *
 * <h2>Repository Implementations</h2>
 * The snapshot producer itself only needs write access to its repositories: it generates pages
 * and an index and hands them to the repository. The repository interfaces only define write access
 * because the producer never needs to read the pages. This allows the repository implementation a lot of flexibility
 * because it only to consider the needs of its own HTTP server when deciding how to save pages.
 *
 * <h3>Consistency Requirements</h3>
 * Repositories must make sure that when the {@link java.util.concurrent.CompletionStage} returned by a
 * repository method succeeds, the data has been successfully saved. Conversely, if saving fails the
 * CompletionStage should also fail.
 * <p>
 * The HTTP server should ensure to only make snapshots available that had their index fully saved, but this is the
 * responsibility of the server implementation and does not impact the producer's use of these repositories.
 */
package io.datareplication.producer.snapshot;
