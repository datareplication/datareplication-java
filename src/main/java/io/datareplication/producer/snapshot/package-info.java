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
 */
package io.datareplication.producer.snapshot;
