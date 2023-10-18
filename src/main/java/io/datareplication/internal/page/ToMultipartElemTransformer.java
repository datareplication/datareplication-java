package io.datareplication.internal.page;

import io.datareplication.internal.multipart.Elem;
import io.datareplication.internal.multipart.MultipartParser;
import io.datareplication.internal.multipart.RequestInput;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

class ToMultipartElemTransformer {
    private final MultipartParser parser;

    private ByteBuffer buffer = null;

    ToMultipartElemTransformer(final MultipartParser parser) {
        this.parser = parser;
    }

    public List<Elem> transform(ByteBuffer next) {
        if (buffer == null || !buffer.hasRemaining()) {
            buffer = next.slice();
        } else {
            final ByteBuffer newBuffer = ByteBuffer.allocate(buffer.remaining() + next.remaining());
            newBuffer.put(buffer);
            newBuffer.put(next.slice());
            newBuffer.position(0);
            buffer = newBuffer;
        }
        return parseAll();
    }

    private List<Elem> parseAll() {
        final ArrayList<Elem> parsed = new ArrayList<>();
        while (buffer.hasRemaining()) {
            try {
                // TODO: why slice?!
                final MultipartParser.Result result = parser.parse(buffer.slice());
                parsed.add(result.elem());
                buffer.position(buffer.position() + result.consumedBytes());
            } catch (RequestInput r) {
                break;
            }
        }

        return parsed;
    }
}
