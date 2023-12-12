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
import reactor.adapter.JdkFlowAdapter;
import reactor.core.publisher.Mono;

import java.util.concurrent.Flow;

@AllArgsConstructor(access = AccessLevel.PACKAGE)
public class FeedConsumerImpl implements FeedConsumer {
    private final PageLoader pageLoader;
    private final FeedPageCrawler feedPageCrawler;

    @Override
    public @NonNull Flow.Publisher<
        @NonNull StreamingPage<@NonNull FeedPageHeader, @NonNull FeedEntityHeader>
        > streamPages(@NonNull final Url url,
                      @NonNull final StartFrom startFrom) {
        // TODO: NextLinks
        Mono<StreamingPage<FeedPageHeader, FeedEntityHeader>> pageMono = Mono
            .fromCompletionStage(feedPageCrawler.crawl(url, startFrom))
            .flatMap(feedPageHeader -> pageLoader.load(feedPageHeader.self().value()))
            .map(this::wrapPage);
        return JdkFlowAdapter.publisherToFlowPublisher(pageMono);
    }

    @Override
    public @NonNull Flow.Publisher<
        @NonNull Entity<@NonNull FeedEntityHeader>
        > streamEntities(@NonNull final Url url,
                         @NonNull final StartFrom startFrom) {
        throw new UnsupportedOperationException("NIY");
    }

    private StreamingPage<FeedPageHeader, FeedEntityHeader> wrapPage(
        StreamingPage<HttpHeaders, HttpHeaders> page
    ) {
        HttpHeaders header = page.header();
        // TODO: Extract Headers
        return new WrappedStreamingPage<>(page,
            new FeedPageHeader(null, null, null, null),
            httpHeaders -> new FeedEntityHeader(null, null, null)
        );
    }
}
