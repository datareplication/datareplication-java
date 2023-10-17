package io.datareplication.internal.multipart;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class BufferingParser {
    private final Function<ByteBuffer, MultipartParser.Result> parseFunction;

    private ByteBuffer buffer = null;

    public BufferingParser(final Function<ByteBuffer, MultipartParser.Result> parseFunction) {
        this.parseFunction = parseFunction;
    }

    public List<Elem> feed(ByteBuffer next) {
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
                final MultipartParser.Result result = parseFunction.apply(buffer.slice());
                parsed.add(result.elem());
                buffer.position(buffer.position() + result.consumedBytes());
            } catch (RequestInput r) {
                break;
            }
        }

        return parsed;
    }
}
