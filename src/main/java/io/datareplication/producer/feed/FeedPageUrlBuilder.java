package io.datareplication.producer.feed;

import io.datareplication.model.PageId;
import io.datareplication.model.Url;
import lombok.NonNull;

public interface FeedPageUrlBuilder {
    @NonNull Url pageUrl(@NonNull PageId pageId);
}
