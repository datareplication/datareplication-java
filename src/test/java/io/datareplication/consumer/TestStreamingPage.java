package io.datareplication.consumer;

import io.datareplication.model.ToHttpHeaders;
import io.reactivex.rxjava3.core.Flowable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import lombok.Value;
import org.reactivestreams.FlowAdapters;

import java.util.List;
import java.util.concurrent.Flow;

@AllArgsConstructor
public class TestStreamingPage<PageHeader extends ToHttpHeaders, EntityHeader extends ToHttpHeaders>
    implements StreamingPage<PageHeader, EntityHeader> {
    private final PageHeader header;
    private final String boundary;
    private final Flow.Publisher<Chunk<EntityHeader>> flow;

    public TestStreamingPage(final PageHeader header,
                             final String boundary,
                             final Flowable<Chunk<EntityHeader>> chunksFlowable) {
        this(header, boundary, FlowAdapters.toFlowPublisher(chunksFlowable));
    }

    public TestStreamingPage(final PageHeader header,
                             final String boundary,
                             final List<Chunk<EntityHeader>> chunks) {
        this(header, boundary, Flowable.fromIterable(chunks));
    }

    @Override
    public @NonNull PageHeader header() {
        return header;
    }

    @Override
    public @NonNull String boundary() {
        return boundary;
    }

    @Override
    public void subscribe(final Flow.Subscriber<? super Chunk<EntityHeader>> subscriber) {
        flow.subscribe(subscriber);
    }
}
