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
 */
package io.datareplication.producer.feed;
