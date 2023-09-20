package io.datareplication.feed.producer;

import io.datareplication.model.PageId;
import io.datareplication.model.Url;
import lombok.NonNull;

public interface FeedPageUrlBuilder {
    // TODO: if we add IDs to the header, pass entire page header?
    @NonNull Url pageUrl(@NonNull PageId pageId);
}
