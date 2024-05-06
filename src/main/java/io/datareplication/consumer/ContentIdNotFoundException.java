package io.datareplication.consumer;

import io.datareplication.consumer.feed.StartFrom;
import io.datareplication.model.Url;
import lombok.EqualsAndHashCode;
import lombok.NonNull;

import java.time.Instant;

/**
 * Thrown when the ContentId is still not found and the lastModified is older than the current entity
 */
@EqualsAndHashCode(callSuper = false)
public class ContentIdNotFoundException extends ConsumerException {
    private final StartFrom.ContentId startFrom;
    private final Url url;
    private final Instant entityLastModified;

    public ContentIdNotFoundException(
        @NonNull StartFrom.ContentId startFrom,
        @NonNull Url url,
        @NonNull Instant entityLastModified
    ) {
        super(
            String.format(
                "FeedPage '%s' does not contain ContentId '%s' before StartFrom.Timestamp: "
                    + "'%s' is older than Entity.LastModified '%s'",
                url.value(),
                startFrom.contentId().value(),
                startFrom.timestamp(),
                entityLastModified
            )
        );
        this.startFrom = startFrom;
        this.url = url;
        this.entityLastModified = entityLastModified;
    }
}
