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
    private long offset = 0;

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
     * Verify that the parser is "at end", i.e. the given input formed a complete multipart document.
     *
     * @throws MultipartException.UnexpectedEndOfInput when the document was incomplete
     * @throws MultipartException In certain cases, parseAll() doesn't throw a parsing error when it encounters a
     *                            problem. In that case, the next call to parseAll() will throw it. If parseAll() isn't
     *                            called again, then finish() will throw that exception.
     */
    public void finish() {
        if (buffer.hasRemaining()) {
            // finish() being called with input remaining can have one of two reasons:
            //  - The final input block had a syntax problem and parseAll() returned all tokens that it could
            //    successfully parse, leaving the problem area in the buffer. In this case, we want to throw the
            //    actual error rather than the generic end-of-input error.
            //  - MultipartParser threw RequestInput. In this case we want to throw the generic end-of-input error.
            // To figure out which case it is, we call parse() once, rethrow any MultipartException, and throw
            // UnexpectedEndOfInput otherwise.
            try {
                parser.parse(buffer);
            } catch (RequestInput ignored) {
            }
            throw new MultipartException.UnexpectedEndOfInput(offset);
        } else if (!parser.isFinished()) {
            // If the parser is not in its end state, we throw UnexpectedEndOfInput even if we consumed all the input
            // we were given. Clearly it was not enough. We needed more.
            throw new MultipartException.UnexpectedEndOfInput(offset);
        }
    }

    private List<Token> parseAll() {
        final ArrayList<Token> parsed = new ArrayList<>();
        while (buffer.hasRemaining()) {
            try {
                final MultipartParser.Result result = parser.parse(buffer);
                parsed.add(result.token());
                offset += result.consumedBytes();
                buffer.position(buffer.position() + result.consumedBytes());
            } catch (MultipartException exc) {
                // If we have already parsed some tokens, we return those. Since we remember our position, the
                // exception we swallow here will be thrown when this method is called again or when finish() is
                // called. This allows us to parse as much as possible even in the face of invalid input.
                if (parsed.isEmpty()) {
                    throw exc;
                } else {
                    break;
                }
            } catch (RequestInput r) {
                break;
            }
        }
        return parsed;
    }
}
