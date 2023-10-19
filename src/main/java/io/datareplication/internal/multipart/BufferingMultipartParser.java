package io.datareplication.internal.multipart;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Parse a multipart document, internally buffering bytes that haven't been consumed yet.
 * {@link #parse(ByteBuffer)} internally calls {@link MultipartParser#parse(ByteBuffer)} until the input is exhausted
 * or the parser requests more input, then buffers any leftover bytes internally.
 */
public class BufferingMultipartParser {
    private final MultipartParser parser;

    private ByteBuffer buffer = ByteBuffer.allocate(0);

     public BufferingMultipartParser(final MultipartParser parser) {
        this.parser = parser;
    }

    /**
     * Feed a buffer of input and return as many tokens as can be parsed from the new buffer and any leftover buffered
     * input.
     *
     * @param next a new buffer of bytes to append to the parser input
     * @return all tokens that could be fully parsed
     */
    public List<Token> parse(ByteBuffer next) {
        if (!buffer.hasRemaining()) {
            buffer = next.slice();
        } else {
            // NB: we probably have to allocate a new buffer each time: since ByteBuffer is mutable, reusing an existing
            // buffer likely breaks references to input we handed out previously (i.e. Elem.Data)
            final ByteBuffer newBuffer = ByteBuffer.allocate(buffer.remaining() + next.remaining());
            newBuffer.put(buffer);
            newBuffer.put(next.slice());
            newBuffer.position(0);
            buffer = newBuffer;
        }
        return parseAll();
    }

    /**
     * Return true if the parser is finished (i.e. has read the closing delimiter) and the buffered input is exhausted.
     */
    public boolean isFinished() {
        return parser.isFinished() && !buffer.hasRemaining();
    }

    private List<Token> parseAll() {
        final ArrayList<Token> parsed = new ArrayList<>();
        while (buffer.hasRemaining()) {
            try {
                final MultipartParser.Result result = parser.parse(buffer);
                parsed.add(result.token());
                buffer.position(buffer.position() + result.consumedBytes());
            } catch (RequestInput r) {
                break;
            }
        }

        return parsed;
    }
}
