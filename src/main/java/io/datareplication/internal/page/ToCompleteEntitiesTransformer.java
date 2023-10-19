package io.datareplication.internal.page;

import io.datareplication.consumer.StreamingPage;
import io.datareplication.model.Body;
import io.datareplication.model.Entity;
import io.datareplication.model.ToHttpHeaders;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.util.Optional;

class ToCompleteEntitiesTransformer<EntityHeader extends ToHttpHeaders> {
    private StreamingPage.Chunk.Header<EntityHeader> currentHeader = null;
    private ByteArrayOutputStream bodyStream = null;
    private WritableByteChannel bodyChannel = null;

    private static final int INITIAL_BUFFER_SIZE = 4096;

    public Optional<Entity<EntityHeader>> transform(StreamingPage.Chunk<EntityHeader> chunk) {
        if (chunk instanceof StreamingPage.Chunk.Header) {
            currentHeader = (StreamingPage.Chunk.Header<EntityHeader>) chunk;
            // TODO: maybe look at content-length header to preallocate the buffer?
            bodyStream = new ByteArrayOutputStream(INITIAL_BUFFER_SIZE);
            bodyChannel = Channels.newChannel(bodyStream);
            return Optional.empty();
        } else if (chunk instanceof StreamingPage.Chunk.BodyChunk) {
            try {
                bodyChannel.write(((StreamingPage.Chunk.BodyChunk<EntityHeader>) chunk).data());
            } catch (IOException e) {
                throw new IllegalStateException("IOException when writing to in-memory buffer; how?", e);
            }
            return Optional.empty();
        } else if (chunk instanceof StreamingPage.Chunk.BodyEnd) {
            // TODO: Alternative implementation: keep a List<ByteBuffer> and build a Body impl from that. Doesn't
            //  require a big new allocation, but is just generally less efficient?
            final Body body = Body.fromBytes(bodyStream.toByteArray(), currentHeader.contentType());
            final Entity<EntityHeader> entity = new Entity<>(currentHeader.header(), body);
            return Optional.of(entity);
        }
        throw new IllegalArgumentException(String.format("unknown subclass of StreamingPage.Chunk %s; bug?", chunk));
    }
}
