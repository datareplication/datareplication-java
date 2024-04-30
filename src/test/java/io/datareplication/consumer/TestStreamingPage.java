package io.datareplication.consumer;

import io.datareplication.model.ContentType;
import io.datareplication.model.ToHttpHeaders;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import reactor.adapter.JdkFlowAdapter;
import reactor.core.publisher.Flux;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Flow;
import java.util.stream.Collectors;

@AllArgsConstructor
public class TestStreamingPage<PageHeader extends ToHttpHeaders, EntityHeader extends ToHttpHeaders>
    implements StreamingPage<PageHeader, EntityHeader> {
    private final PageHeader header;
    private final String boundary;
    private final Flow.Publisher<Chunk<EntityHeader>> flow;

    public TestStreamingPage(final PageHeader header,
                             final String boundary,
                             final Flux<Chunk<EntityHeader>> chunksFlux) {
        this(header, boundary, JdkFlowAdapter.publisherToFlowPublisher(chunksFlux));
    }

    public TestStreamingPage(final PageHeader header,
                             final String boundary,
                             final List<Chunk<EntityHeader>> chunks) {
        this(header, boundary, Flux.fromIterable(chunks));
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


    @SafeVarargs
    public static <PageHeader extends ToHttpHeaders, EntityHeader extends ToHttpHeaders>
    TestStreamingPage<PageHeader, EntityHeader> testStreamingPageOf(
        final PageHeader pageHeader,
        final String boundary,
        final TestEntity<EntityHeader>... entityHttpHeaders) {
        var chunks = Arrays
            .stream(entityHttpHeaders)
            .map(entity ->
                List.<Chunk<EntityHeader>>of(
                    StreamingPage.Chunk.header(entity.httpHeaders, ContentType.of("text/plain")),
                    StreamingPage.Chunk.bodyChunk(ByteBuffer.wrap(entity.entity.getBytes(StandardCharsets.UTF_8))),
                    StreamingPage.Chunk.bodyEnd()
                )
            )
            .flatMap(List<Chunk<EntityHeader>>::stream)
            .collect(Collectors.toList());
        return new TestStreamingPage<>(pageHeader, boundary, chunks);
    }

    @AllArgsConstructor(staticName = "of")
    public static class TestEntity<EntityHeader> {
        private final EntityHeader httpHeaders;
        private final String entity;
    }
}
