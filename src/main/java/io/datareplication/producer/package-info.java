/**
 * TODO: move all this to the respective submodules because this package can't be navigated to
 * <h2>Repository Implementations</h2>
 * The feed and snapshot producers use repositories in somewhat different ways: the snapshot producer generates pages
 * and an index and hands them one by one to the repository. The repository interfaces only define write access
 * because the producer never needs to read the pages. This allows the repository implementation to be quite flexible
 * and save only the parts of pages that it needs for serving them over HTTP.
 * <p>
 * In contrast, the interfaces for the feed producer repositories are more complex and define multiple read methods
 * in addition to the save methods. In general, these repositories need to be able to accurately reproduce any
 * previously stored records so it's important they save and load all fields in their parameters.
 *
 * <h3>Consistency Requirements</h3>
 * Repository implementations need to follow certain consistency rules to ensure that the producers work correctly
 * (this is significantly more important for the feed producer). These are described in the documentation for each
 * repository under the heading "Consistency Requirements". If a repository implementation doesn't uphold the rules
 * of its interface, the producer may produce invalid output or otherwise misbehave.
 */
package io.datareplication.producer;
