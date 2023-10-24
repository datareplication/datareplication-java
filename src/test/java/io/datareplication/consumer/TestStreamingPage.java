package io.datareplication.consumer;

import io.datareplication.model.ToHttpHeaders;
import io.reactivex.rxjava3.core.Flowable;
import lombok.NonNull;
import lombok.Value;
import org.reactivestreams.FlowAdapters;

import java.util.List;
import java.util.concurrent.Flow;

@Value
public class TestStreamingPage<PageHeader extends ToHttpHeaders, EntityHeader extends ToHttpHeaders>
    implements StreamingPage<PageHeader, EntityHeader> {
    @NonNull PageHeader header;
    @NonNull String boundary;
    @NonNull List<Chunk<EntityHeader>> chunks;

    @Override
    public void subscribe(final Flow.Subscriber<? super Chunk<EntityHeader>> subscriber) {
        final Flowable<Chunk<EntityHeader>> flowable = Flowable.fromIterable(chunks);
        FlowAdapters.toFlowPublisher(flowable).subscribe(subscriber);
    }
}
