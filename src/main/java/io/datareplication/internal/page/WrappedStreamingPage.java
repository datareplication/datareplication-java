package io.datareplication.internal.page;

import io.datareplication.consumer.StreamingPage;
import io.datareplication.model.HttpHeaders;
import io.datareplication.model.ToHttpHeaders;
import lombok.NonNull;
import reactor.adapter.JdkFlowAdapter;

import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;

/**
 * {@link StreamingPage} impl that wraps a StreamingPage containing generic {@link HttpHeaders} with specialized header
 * types using a conversion function.
 * @param <PageHeader> the page header output type
 * @param <EntityHeader> the entity header output type
 */
public class WrappedStreamingPage<PageHeader extends ToHttpHeaders, EntityHeader extends ToHttpHeaders>
    implements StreamingPage<PageHeader, EntityHeader> {
    private final PageHeader pageHeader;
    private final String boundary;
    private final Flow.Publisher<StreamingPage.Chunk<EntityHeader>> mappedPublisher;

    @SuppressWarnings("unchecked")
    public WrappedStreamingPage(final StreamingPage<HttpHeaders, HttpHeaders> underlying,
                                final PageHeader pageHeader,
                                final BiFunction<Integer, HttpHeaders, EntityHeader> convertEntityHeader) {
        this.pageHeader = pageHeader;
        this.boundary = underlying.boundary();
        final AtomicInteger index = new AtomicInteger();
        var flux = JdkFlowAdapter
            .flowPublisherToFlux(underlying)
            .map(chunk -> {
                if (chunk instanceof StreamingPage.Chunk.Header) {
                    var header = (StreamingPage.Chunk.Header<HttpHeaders>) chunk;
                    var convertedHeader = convertEntityHeader.apply(index.getAndIncrement(), header.header());
                    return Chunk.header(convertedHeader, header.contentType());
                } else {
                    // this is ok: the only subclass that uses the type parameter is Header which we explicitly convert
                    // above
                    return (Chunk<EntityHeader>) chunk;
                }
            });
        this.mappedPublisher = JdkFlowAdapter.publisherToFlowPublisher(flux);
    }

    @Override
    public @NonNull PageHeader header() {
        return pageHeader;
    }

    @Override
    public @NonNull String boundary() {
        return boundary;
    }

    @Override
    public void subscribe(final Flow.Subscriber<? super Chunk<EntityHeader>> subscriber) {
        mappedPublisher.subscribe(subscriber);
    }
}
