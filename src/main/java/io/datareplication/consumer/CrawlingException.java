package io.datareplication.consumer;

import io.datareplication.model.Timestamp;
import io.datareplication.model.Url;

/**
 * Thrown when the last available FeedPage is not older than the given last modified date.
 */
public class CrawlingException extends ConsumerException {
    public CrawlingException(Url url, Timestamp feedPageLastModified, Timestamp lastModified) {
        super(
            String.format(
                "Last crawled FeedPage '%s' date '%s' is not older than LastModified: '%s'",
                url.value(),
                feedPageLastModified.value(),
                lastModified.value()
            )
        );
    }
}
