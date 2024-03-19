package io.datareplication.consumer.feed;

import io.datareplication.consumer.StreamingPage;
import io.datareplication.internal.page.PageLoader;
import io.datareplication.internal.page.WrappedStreamingPage;
import io.datareplication.model.Entity;
import io.datareplication.model.HttpHeaders;
import io.datareplication.model.Url;
import io.datareplication.model.feed.FeedEntityHeader;
import io.datareplication.model.feed.FeedPageHeader;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import org.reactivestreams.FlowAdapters;
import reactor.adapter.JdkFlowAdapter;
import reactor.core.publisher.Flux;

import java.util.concurrent.Flow;

@AllArgsConstructor(access = AccessLevel.PACKAGE)
public class FeedConsumerImpl implements FeedConsumer {
    private final PageLoader pageLoader;
    private final FeedPageCrawler feedPageCrawler;
    private final FeedPageHeaderParser feedPageHeaderParser;

    @Override
    public @NonNull Flow.Publisher<
        @NonNull StreamingPage<@NonNull FeedPageHeader, @NonNull FeedEntityHeader>
        > streamPages(@NonNull final Url url,
                      @NonNull final StartFrom startFrom) {
        return JdkFlowAdapter.publisherToFlowPublisher(streamPagesFlux(url, startFrom));
    }

    @Override
    public @NonNull Flow.Publisher<
        @NonNull Entity<@NonNull FeedEntityHeader>
        > streamEntities(@NonNull final Url url,
                         @NonNull final StartFrom startFrom) {
        var entityFlux = streamPagesFlux(url, startFrom)
            .map(StreamingPage::toCompleteEntities)
            .map(FlowAdapters::toPublisher)
            .flatMap(Flux::from);
        return JdkFlowAdapter.publisherToFlowPublisher(entityFlux);
    }

    private @NonNull Flux<@NonNull StreamingPage<@NonNull FeedPageHeader,
        @NonNull FeedEntityHeader>
        > streamPagesFlux(@NonNull final Url url,
                          @NonNull final StartFrom startFrom) {
        // TODO: Follow next links
        return Flux.concat(feedPageCrawler
            .crawl(url, startFrom)
            .flatMap(feedPageHeader -> pageLoader.load(feedPageHeader.self().value()))
            .map(this::wrapPage));
    }

    private @NonNull StreamingPage<@NonNull FeedPageHeader,
        @NonNull FeedEntityHeader
        > wrapPage(@NonNull StreamingPage<@NonNull HttpHeaders,
        @NonNull HttpHeaders> page) {
        return new WrappedStreamingPage<>(
            page,
            feedPageHeaderParser.feedPageHeader(page.header()),
            feedPageHeaderParser::feedEntityHeader
        );
    }
}
