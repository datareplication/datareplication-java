package io.datareplication.producer.feed;

import io.datareplication.model.ContentType;
import io.datareplication.model.Page;
import io.datareplication.model.PageId;
import io.datareplication.model.feed.FeedEntityHeader;
import io.datareplication.model.feed.FeedPageHeader;
import lombok.NonNull;
import lombok.Value;

import java.util.Optional;
import java.util.concurrent.CompletionStage;

public interface FeedPageProvider {
    // TODO: alternative: put content-* stuff into extraHeaders
    // TODO: better name?
    @Value
    public class FeedPageHeaderWithContentType {
        @NonNull FeedPageHeader header;
        @NonNull ContentType contentType;
        long contentLength;
    }

    @NonNull CompletionStage<@NonNull Optional<@NonNull PageId>> latestPageId();

    @NonNull CompletionStage<@NonNull Optional<@NonNull FeedPageHeaderWithContentType>> pageHeader(@NonNull PageId id);

    @NonNull CompletionStage<@NonNull Optional<@NonNull Page<@NonNull FeedPageHeader, @NonNull FeedEntityHeader>>> page(
            @NonNull PageId id);
}
