package io.datareplication.producer.feed;

import io.datareplication.model.*;
import io.datareplication.model.feed.FeedEntityHeader;
import io.datareplication.model.feed.FeedPageHeader;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Value;

import java.util.Optional;
import java.util.concurrent.CompletionStage;

// TODO: what's a better name for this class?
public interface FeedPageProvider {
    // About the question of "what headers on HEAD": the accepted answer states the opposite, but according to linked
    // https://www.rfc-editor.org/rfc/rfc7231#section-4.3.2, HEAD requests may omit the content-length header. This is,
    // uh, useful because we don't know the real content-length without loading all entities (the numberOfBytes field in
    // the page metadata is wrong because it only includes entity bodies). However, it looks like content-type will have
    // to be included so we can't just use FeedPageHeader.
    // https://stackoverflow.com/questions/3854842/content-length-header-with-head-requests

    @Value
    class HeaderAndContentType implements ToHttpHeaders {
        @NonNull FeedPageHeader header;
        @NonNull ContentType contentType;

        @Override
        public @NonNull HttpHeaders toHttpHeaders() {
            return header
                .toHttpHeaders()
                .update(HttpHeader.contentType(contentType));
        }
    }

    @NonNull CompletionStage<@NonNull Optional<@NonNull PageId>> latestPageId();

    @NonNull CompletionStage<@NonNull Optional<@NonNull HeaderAndContentType>> pageHeader(@NonNull PageId id);

    @NonNull CompletionStage<@NonNull Optional<@NonNull Page<@NonNull FeedPageHeader, @NonNull FeedEntityHeader>>> page(
        @NonNull PageId id);

    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    class Builder {
        private final FeedEntityRepository feedEntityRepository;
        private final FeedPageMetadataRepository feedPageMetadataRepository;
        private final FeedPageUrlBuilder feedPageUrlBuilder;

        public @NonNull FeedPageProvider build() {
            return new FeedPageProviderImpl(
                feedEntityRepository,
                feedPageMetadataRepository,
                feedPageUrlBuilder
            );
        }
    }

    static @NonNull Builder builder(@NonNull FeedEntityRepository feedEntityRepository,
                                    @NonNull FeedPageMetadataRepository feedPageMetadataRepository,
                                    @NonNull FeedPageUrlBuilder feedPageUrlBuilder) {
        return new Builder(
            feedEntityRepository,
            feedPageMetadataRepository,
            feedPageUrlBuilder
        );
    }
}
