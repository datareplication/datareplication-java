package io.datareplication.consumer;

import io.datareplication.model.Url;

import java.time.Instant;

/**
 * Thrown when the last available FeedPage is not older than the given last modified date.
 */
public class CrawlingException extends ConsumerException {
    public CrawlingException(Url url, Instant feedPageLastModified, Instant lastModified) {
        super(
            String.format(
                "Last crawled FeedPage '%s' date '%s' is not older than LastModified: '%s'",
                url.value(),
                feedPageLastModified,
                lastModified
            )
        );
    }
}
