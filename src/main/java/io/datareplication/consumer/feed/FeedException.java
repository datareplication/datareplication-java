package io.datareplication.consumer.feed;

import io.datareplication.consumer.ConsumerException;
import io.datareplication.model.Url;
import lombok.EqualsAndHashCode;
import lombok.NonNull;

import java.time.Instant;

public abstract class FeedException extends ConsumerException {
    private FeedException(@NonNull final String message) {
        super(message);
    }

    @EqualsAndHashCode(callSuper = false)
    public static final class ContentIdNotFound extends FeedException {
        private final StartFrom.ContentId startFrom;
        private final Url url;
        private final Instant entityLastModified;

        public ContentIdNotFound(
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

    @EqualsAndHashCode(callSuper = false)
    public static final class FeedNotOldEnough extends FeedException {
        private final Url url;
        private final Instant feedPageLastModified;
        private final Instant requestedLastModified;

        public FeedNotOldEnough(
            @NonNull final Url url,
            @NonNull final Instant feedPageLastModified,
            @NonNull final Instant requestedLastModified
        ) {
            super(String.format(
                "Last crawled FeedPage '%s' date '%s' is not older than requested LastModified: '%s'",
                url.value(),
                feedPageLastModified,
                requestedLastModified
            ));
            this.url = url;
            this.feedPageLastModified = feedPageLastModified;
            this.requestedLastModified = requestedLastModified;
        }
    }
}
