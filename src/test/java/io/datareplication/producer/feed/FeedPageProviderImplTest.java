package io.datareplication.producer.feed;

import io.datareplication.model.*;
import io.datareplication.model.feed.FeedPageHeader;
import io.datareplication.model.feed.Link;
import lombok.NonNull;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FeedPageProviderImplTest {
    private final FeedEntityRepository feedEntityRepository = mock(FeedEntityRepository.class);
    private final FeedPageMetadataRepository feedPageMetadataRepository = mock(FeedPageMetadataRepository.class);
    private final FeedPageUrlBuilder feedPageUrlBuilder = new FeedPageUrlBuilder() {
        @Override
        public @NonNull Url pageUrl(@NonNull PageId pageId) {
            return Url.of( String.format("https://datareplication.io/%s", pageId.value()));
        }
    };

    private final FeedPageProvider feedPageProvider = new FeedPageProviderImpl(
        feedEntityRepository,
        feedPageMetadataRepository,
        feedPageUrlBuilder
    );

    @Test
    void latestPageId_shouldReturnNoPageId_whenNoPagesWithoutNextLink() {
        when(feedPageMetadataRepository.getWithoutNextLink())
            .thenReturn(Mono.just(Collections.<FeedPageMetadataRepository.PageMetadata>emptyList()).toFuture());

        var result = feedPageProvider.latestPageId();

        StepVerifier
            .create(Mono.fromCompletionStage(result))
            .expectNext(Optional.empty())
            .expectComplete()
            .verify();
    }

    @Test
    void latestPageId_shouldReturnPageId_whenExactlyOnePageWithoutNextLink() {
        when(feedPageMetadataRepository.getWithoutNextLink())
            .thenReturn(Mono.just(List.of(page("page1", 4))).toFuture());

        var result = feedPageProvider.latestPageId();

        StepVerifier
            .create(Mono.fromCompletionStage(result))
            .expectNext(Optional.of(PageId.of("page1")))
            .expectComplete()
            .verify();
    }

    @Test
    void latestPageId_shouldReturnPageIdWithLowestGeneration_whenMultiplePagesWithoutNextLink() {
        when(feedPageMetadataRepository.getWithoutNextLink()).thenReturn(Mono.just(List.of(
            page("page1", 4),
            page("page2", 3),
            page("page3", 6)
        )).toFuture());

        var result = feedPageProvider.latestPageId();

        StepVerifier
            .create(Mono.fromCompletionStage(result))
            .expectNext(Optional.of(PageId.of("page2")))
            .expectComplete()
            .verify();
    }

    @Test
    void pageHeader_shouldReturnEmpty_whenNoPageMetadataForId() {
        var pageId = PageId.of("page");
        when(feedPageMetadataRepository.get(pageId))
            .thenReturn(Mono.just(Optional.<FeedPageMetadataRepository.PageMetadata>empty()).toFuture());

        var result = feedPageProvider.pageHeader(pageId);

        StepVerifier
            .create(Mono.fromCompletionStage(result))
            .expectNext(Optional.empty())
            .expectComplete()
            .verify();
    }

    @Test
    void pageHeader_shouldReturnPageHeaderWithoutPrevAndNextLinks() {
        var pageId = PageId.of("page");
        var timestamp = Timestamp.of(Instant.parse("2024-02-02T14:52:32Z"));
        when(feedPageMetadataRepository.get(pageId))
            .thenReturn(Mono.just(Optional.of(new FeedPageMetadataRepository.PageMetadata(
                PageId.of("pageid-from-repo"),
                timestamp,
                Optional.empty(),
                Optional.empty(),
                666,
                13,
                900
            ))).toFuture());

        var result = feedPageProvider.pageHeader(pageId);

        StepVerifier
            .create(Mono.fromCompletionStage(result))
            .expectNext(Optional.of(new FeedPageProvider.HeaderWithContentType(
                new FeedPageHeader(
                    timestamp,
                    Link.self(Url.of("https://datareplication.io/pageid-from-repo")),
                    Optional.empty(),
                    Optional.empty(),
                    HttpHeaders.EMPTY
                ),
                ContentType.of("multipart/mixed; boundary=\"_---_pageid-from-repo\"")
            )))
            .expectComplete()
            .verify();
    }

    @Test
    void pageHeader_shouldReturnPageHeaderWithPrevAndNextLinks() {
        var pageId = PageId.of("page-for-get");
        var timestamp = Timestamp.of(Instant.parse("2024-02-02T15:07:00Z"));
        when(feedPageMetadataRepository.get(pageId))
            .thenReturn(Mono.just(Optional.of(new FeedPageMetadataRepository.PageMetadata(
                PageId.of("pageid"),
                timestamp,
                Optional.of(PageId.of("prev-page")),
                Optional.of(PageId.of("next-page")),
                123,
                9313,
                1
            ))).toFuture());

        var result = feedPageProvider.pageHeader(pageId);

        StepVerifier
            .create(Mono.fromCompletionStage(result))
            .expectNext(Optional.of(new FeedPageProvider.HeaderWithContentType(
                new FeedPageHeader(
                    timestamp,
                    Link.self(Url.of("https://datareplication.io/pageid")),
                    Optional.of(Link.prev(Url.of("https://datareplication.io/prev-page"))),
                    Optional.of(Link.next(Url.of("https://datareplication.io/next-page"))),
                    HttpHeaders.EMPTY
                ),
                ContentType.of("multipart/mixed; boundary=\"_---_pageid\"")
            )))
            .expectComplete()
            .verify();
    }

    private FeedPageMetadataRepository.PageMetadata page(String pageId, int generation) {
        return new FeedPageMetadataRepository.PageMetadata(
            PageId.of(pageId),
            Timestamp.of(Instant.parse("2024-01-30T16:24:00Z")),
            Optional.empty(),
            Optional.empty(),
            15,
            1,
            generation
        );
    }
}
