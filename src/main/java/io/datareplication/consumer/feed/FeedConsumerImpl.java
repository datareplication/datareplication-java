package io.datareplication.consumer.feed;

import io.datareplication.consumer.StreamingPage;
import io.datareplication.internal.page.PageLoader;
import io.datareplication.internal.page.WrappedStreamingPage;
import io.datareplication.model.Entity;
import io.datareplication.model.HttpHeaders;
import io.datareplication.model.Url;
import io.datareplication.model.feed.ContentId;
import io.datareplication.model.feed.FeedEntityHeader;
import io.datareplication.model.feed.FeedPageHeader;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import org.reactivestreams.FlowAdapters;
import org.reactivestreams.Publisher;
import reactor.adapter.JdkFlowAdapter;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.concurrent.Flow;

/**
 * A consumer for a Feed provided by the {@link io.datareplication.producer.feed.FeedProducer}.
 *
 * @see FeedConsumer
 */
@AllArgsConstructor(access = AccessLevel.PACKAGE)
class FeedConsumerImpl implements FeedConsumer {
    private final PageLoader pageLoader;
    private final FeedPageCrawler feedPageCrawler;
    private final FeedPageHeaderParser feedPageHeaderParser;

    /**
     * @see FeedConsumer#streamPages(Url, StartFrom)
     */
    @Override
    public @NonNull Flow.Publisher<@NonNull StreamingPage<@NonNull FeedPageHeader, @NonNull FeedEntityHeader>>
    streamPages(@NonNull final Url url, @NonNull final StartFrom startFrom) {
        return JdkFlowAdapter.publisherToFlowPublisher(streamPagesFlux(url, startFrom));
    }

    /**
     * @see FeedConsumer#streamEntities(Url, StartFrom)
     */
    @Override
    public @NonNull Flow.Publisher<@NonNull Entity<@NonNull FeedEntityHeader>>
    streamEntities(@NonNull final Url url, @NonNull final StartFrom startFrom) {
        var entityFlux = streamPagesFlux(url, startFrom)
            .map(StreamingPage::toCompleteEntities)
            .map(FlowAdapters::toPublisher)
            .flatMap(Flux::from);

        return JdkFlowAdapter.publisherToFlowPublisher(applyStartFrom(startFrom, entityFlux));
    }

    private @NonNull Publisher<Entity<FeedEntityHeader>> applyStartFrom(
        @NonNull final StartFrom startFrom,
        @NonNull final Flux<Entity<FeedEntityHeader>> entityFlux) {
        var startFromFlux = entityFlux;
        if (startFrom instanceof StartFrom.Timestamp || startFrom instanceof StartFrom.ContentId) {
            startFromFlux = startFromFlux.skipUntil(entity -> skipUntil(entity.header(), startFrom));
        }
        if (startFrom instanceof StartFrom.ContentId) {
            startFromFlux = startFromFlux.filter(entity -> !containsContentId(entity.header().contentId(), startFrom));
        }
        return startFromFlux;
    }

    private boolean containsContentId(final ContentId contentId, final StartFrom startFrom) {
        if (startFrom instanceof StartFrom.ContentId) {
            return ((StartFrom.ContentId) startFrom).contentId().equals(contentId);
        } else {
            return false;
        }
    }

    private boolean skipUntil(final FeedEntityHeader header, final StartFrom startFrom) {
        if (startFrom instanceof StartFrom.Timestamp) {
            return !header.lastModified().isBefore(((StartFrom.Timestamp) startFrom).timestamp());
        } else if (startFrom instanceof StartFrom.ContentId) {
            return ((StartFrom.ContentId) startFrom).contentId().equals(header.contentId());
        } else {
            return true;
        }
    }

    private @NonNull Flux<@NonNull StreamingPage<@NonNull FeedPageHeader, @NonNull FeedEntityHeader>>
    streamPagesFlux(@NonNull final Url url, @NonNull final StartFrom startFrom) {
        return feedPageCrawler
            .crawl(url, startFrom)
            .flatMap(pageLoader::load)
            .map(this::wrapPage)
            .expand(this::expandNextPageIfExists);
    }

    private @NonNull StreamingPage<@NonNull FeedPageHeader, @NonNull FeedEntityHeader>
    wrapPage(@NonNull StreamingPage<@NonNull HttpHeaders,
        @NonNull HttpHeaders> page) {
        return new WrappedStreamingPage<>(
            page,
            feedPageHeaderParser.feedPageHeader(page.header()),
            feedPageHeaderParser::feedEntityHeader
        );
    }

    private Mono<@NonNull StreamingPage<@NonNull FeedPageHeader, @NonNull FeedEntityHeader>>
    expandNextPageIfExists(@NonNull final StreamingPage<@NonNull FeedPageHeader, @NonNull FeedEntityHeader> page) {
        return page
            .header()
            .next()
            .map(next -> pageLoader
                .load(next.value())
                .map(this::wrapPage))
            .orElseGet(Mono::empty);
    }
}
