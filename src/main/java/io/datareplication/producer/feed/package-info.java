/**
 * The feed producer builds a linear feed from incoming entities across multiple distributed producer instances.
 * <p>
 * The feed producer provides the following guarantees for the generated feed (as long as there are no bugs in either
 * the producer or the repository implementations): (TODO rewrite/expand)
 * <ul>
 *     <li>consistent feed: monotonously increasing timestamps, matching headers</li>
 *     <li>fully atomic feed updates: no intermediate states are visible</li>
 *     <li>entities published on a single producer instance will maintain their relative order</li>
 *     <li>crash resilience: crashes will never corrupt or lose entities or already-consumable feed pages</li>
 * </ul>
 *
 * <h2>Usage</h2>
 * TODO
 *
 * <h2>Repository Implementations</h2>
 * The feed producer needs both read and write access to its repositories. When running a distributed producer across
 * multiple processes or machines, the repositories are how the instances share their state.
 * <p>
 * The feed producer relies on certain guarantees from the repository implementations. It's important to uphold
 * these guarantees when implementing the repositories. Bugs in the repository implementations can break the
 * producer's consistency guarantees and lead to inconsistent feeds or other data corruption.
 *
 * <h3>Consistency Requirements</h3>
 * Repositories must make sure that when the {@link java.util.concurrent.CompletionStage} returned by a
 * repository method succeeds, all data passed to the method has been successfully saved. Conversely,
 * if saving fails the CompletionStage must also fail.
 * <p>
 * When the CompletionStage for a write operation has succeeded, <em>all data written by the write operation must be
 * visible to all future read operations across all producer instances</em>. Most non-distributed databases work this way
 * by default, but an eventually consistent data store is <strong>not</strong> sufficient on its own and might need
 * some extra handling by e.g. waiting for the write to be acknowledged across all instances.
 * <p>
 * In general, write operations on repositories are required to be <em>atomic at the level of individual records</em>.
 * This means that any time a method takes a list of multiple records to save or update, until the write operation
 * has finished and its CompletionStage has succeeded:
 * <ul>
 * <li>read operations may observe both old as well as already updated records</li>
 * <li>the order in which updates are performed is undefined, i.e. repository implementations may choose to
 *     update records in whichever order is convenient for them</li>
 * <li>if saving a record fails, the CompletionStage associated with the operation must also fail, but already
 *     updated records don't have to be rolled back</li>
 * <li>however, each individual record must either be updated or not: observing partially-updated records
 *     must <strong>not</strong> be possible</li>
 * </ul>
 * Implementing stricter behaviour is fine: for example, in an SQL database it's ok to wrap the entire save
 * operation in a transaction to make it atomic across all records. There's no benefit to doing this however.
 *
 * <h3>Timestamp Precision</h3>
 * The timestamp type used by the library ({@link java.time.Instant}) has nanosecond precision. Most
 * operating system clocks have less precision than that so collected timestamps won't use the full value range.
 * However, repositories should still store timestamps at their full nanosecond precision.
 */
package io.datareplication.producer.feed;
