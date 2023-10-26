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

/**
 * Transform a stream of {@link StreamingPage.Chunk} objects into complete {@link Entity Entities}. This class mostly
 * collects body bytes internally until the end of an entity is signaled and then returns the complete entity with its
 * entire body.
 * @param <EntityHeader> the header type for the entities; this is just passed through
 */
public class ToCompleteEntitiesTransformer<EntityHeader extends ToHttpHeaders> {
    // these fields are null on construction, but they get initialized when we start our first entity
    private StreamingPage.Chunk.Header<EntityHeader> currentHeader;
    private ByteArrayOutputStream bodyStream;
    private WritableByteChannel bodyChannel;

    private static final int INITIAL_BUFFER_SIZE = 4096;

    /**
     * <p>Consume a {@link StreamingPage.Chunk} and return a complete {@link Entity} if we finished one.</p>
     *
     * <p>NB: feeding chunks out of the expected order (e.g. bytes without a header first) is Not Allowed.
     * Don't Do That. In particular, it might cause NPEs.</p>
     * @param chunk a {@link StreamingPage.Chunk}
     * @return an {@link Entity} if this chunk finished one
     */
    public Optional<Entity<EntityHeader>> transform(StreamingPage.Chunk<EntityHeader> chunk) {
        if (chunk instanceof StreamingPage.Chunk.Header) {
            currentHeader = (StreamingPage.Chunk.Header<EntityHeader>) chunk;
            // TODO: Maybe look at content-length header to preallocate the buffer? Could do some special-casing so
            //       we don't always have to call toHttpHeaders...
            //       In that case, limit it to avoid the DoS case of huge content-length.
            bodyStream = new ByteArrayOutputStream(INITIAL_BUFFER_SIZE);
            bodyChannel = Channels.newChannel(bodyStream);
            return Optional.empty();
        } else if (chunk instanceof StreamingPage.Chunk.BodyChunk) {
            try {
                bodyChannel.write(((StreamingPage.Chunk.BodyChunk<EntityHeader>) chunk).data());
            } catch (IOException e) {
                throw new IllegalStateException("IOException when writing to in-memory buffer; weird; bug?", e);
            }
            return Optional.empty();
        } else if (chunk instanceof StreamingPage.Chunk.BodyEnd) {
            // TODO: Alternative implementation: keep a List<ByteBuffer> and build a Body impl from that. Doesn't
            //  require a big new allocation, but feels just generally less efficient?
            final Body body = Body.fromBytes(bodyStream.toByteArray(), currentHeader.contentType());
            final Entity<EntityHeader> entity = new Entity<>(currentHeader.header(), body);
            return Optional.of(entity);
        }
        throw new IllegalArgumentException(String.format("unknown subclass of StreamingPage.Chunk %s; bug?", chunk));
    }
}
