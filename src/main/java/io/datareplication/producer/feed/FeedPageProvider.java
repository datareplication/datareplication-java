package io.datareplication.producer.feed;

import io.datareplication.model.Page;
import io.datareplication.model.PageId;
import io.datareplication.model.feed.FeedEntityHeader;
import io.datareplication.model.feed.FeedPageHeader;
import lombok.NonNull;

import java.util.Optional;
import java.util.concurrent.CompletionStage;

public interface FeedPageProvider {
    @NonNull CompletionStage<@NonNull Optional<@NonNull FeedPageHeader>> pageHeader(@NonNull PageId id);

    @NonNull CompletionStage<@NonNull Optional<@NonNull FeedPageHeader>> latestPageHeader();

    @NonNull CompletionStage<@NonNull Optional<@NonNull Page<@NonNull FeedPageHeader, @NonNull FeedEntityHeader>>> page(
            @NonNull PageId id);

    @NonNull CompletionStage<@NonNull Optional<@NonNull Page<@NonNull FeedPageHeader, @NonNull FeedEntityHeader>>> latestPage();
}
