package io.datareplication.internal.multipart;

import lombok.NonNull;
import lombok.Value;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * Parse a multipart document into {@link Token Tokens}.
 */
public class MultipartParser {
    private State state;
    private long offset;
    private final ByteBuffer dashBoundary;
    private final CharsetDecoder headerDecoder;

    private static final ByteBuffer CLOSE_DELIMITER = ByteBuffer.wrap("--".getBytes(StandardCharsets.US_ASCII));

    @Value
    public static class Result {
        @NonNull Token token;
        int consumedBytes;

        private static Result data(ByteBuffer data) {
            return new Result(new Token.Data(data), data.limit());
        }
    }

    private enum State {
        PREAMBLE,
        PART_BEGIN,
        HEADERS,
        DATA,
        EPILOGUE,
    }

    public MultipartParser(@NonNull ByteBuffer boundary, @NonNull Charset headerCharset) {
        state = State.PREAMBLE;
        offset = 0;
        headerDecoder = headerCharset
                .newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT);
        dashBoundary = ByteBuffer.allocate(boundary.capacity() + 2);
        dashBoundary.put((byte) '-');
        dashBoundary.put((byte) '-');
        dashBoundary.put(boundary);
    }

    public MultipartParser(ByteBuffer boundary) {
        this(boundary, StandardCharsets.UTF_8);
    }

    /**
     * Parse the next token from the input
     * @param input the ByteBuffer; it's sliced before parsing, meaning only bytes between <code>position()</code> and
     *              <code>limit()</code> are read
     * @throws RequestInput the input doesn't contain enough bytes to unambiguously determine the next token; parse()
     *                      must be called again with a buffer that contains additional input as well as the bytes from
     *                      this call
     * @throws MultipartException something went wrong
     *
     * @return the parsed result
     */
    public @NonNull Result parse(@NonNull ByteBuffer input) {
        Result result = parseInternal(input.slice());
        offset += result.consumedBytes;
        return result;
    }

    private Result parseInternal(final ByteBuffer input) {
        switch (state) {
            case PREAMBLE:
                return Combinators
                        .tag(dashBoundary)
                        .parse(input, 0)
                        .map(pos -> {
                            state = State.PART_BEGIN;
                            return new Result(Token.Continue.INSTANCE, pos.end());
                        })
                        .or(() -> Combinators
                                .scan(Combinators.eol())
                                .parse(input, 0)
                                .map(pos -> new Result(Token.Continue.INSTANCE, pos.end())))
                        .orElseGet(() -> new Result(Token.Continue.INSTANCE, input.limit()));
            case PART_BEGIN:
                return Combinators
                        .eol()
                        .parse(input, 0)
                        .map(pos -> {
                            state = State.HEADERS;
                            return new Result(Token.PartBegin.INSTANCE, pos.end());
                        })
                        .or(() -> Combinators
                                .tag(CLOSE_DELIMITER)
                                .parse(input, 0)
                                .map(pos -> {
                                    state = State.EPILOGUE;
                                    return new Result(Token.Continue.INSTANCE, pos.end());
                                }))
                        .orElseThrow(() -> new MultipartException.InvalidDelimiter(offset));
            case HEADERS:
                final Combinators.Pos eol = Combinators
                        .scan(Combinators.eol())
                        .parse(input, 0)
                        .orElseThrow(RequestInput::new);
                if (eol.start() == 0) {
                    // immediate newline, go to body
                    state = State.DATA;
                    return new Result(Token.DataBegin.INSTANCE, eol.end());
                } else {
                    final ByteBuffer headerLine = input.slice().limit(eol.start());
                    return new Result(parseHeader(headerLine), eol.end());
                }
            case DATA:
                return Combinators
                        .seq(Combinators.eol(), Combinators.tag(dashBoundary))
                        .parse(input, 0)
                        .map(pos -> {
                            state = State.PART_BEGIN;
                            return new Result(Token.PartEnd.INSTANCE, pos.end());
                        })
                        .or(() -> Combinators
                                .scan(Combinators.eol())
                                .parse(input, 0)
                                // We know that any EOL at index 0 is not a delimiter (because if we're in this branch,
                                // we already checked for that and didn't find it). This means we can skip past this EOL
                                // to the next one so we can make some progress.
                                .flatMap(pos -> pos.start() == 0
                                        ? Combinators.scan(Combinators.eol()).parse(input, pos.end())
                                        : Optional.of(pos))
                                .map(pos -> Result.data(input.slice().limit(pos.start()))))
                        .orElseGet(() -> Result.data(input.slice()));
            case EPILOGUE:
                return new Result(Token.Continue.INSTANCE, input.limit());
            default:
                throw new IllegalStateException(String.format("unknown state %s; bug in parser?", state));
        }
    }

    private Token.Header parseHeader(ByteBuffer bytes) {
        final String headerString;
        try {
            headerString = headerDecoder.decode(bytes).toString();
        } catch (CharacterCodingException cause) {
            final MultipartException exc = new MultipartException.UndecodableHeader(headerDecoder.charset(), offset);
            exc.initCause(cause);
            throw exc;
        }
        int idx = headerString.indexOf(':');
        if (idx == -1) {
            throw new MultipartException.InvalidHeader(headerString, offset);
        }
        String name = headerString.substring(0, idx).trim();
        String value = headerString.substring(idx + 1).trim();
        return new Token.Header(name, value);
    }

    /**
     * Return true if a closing delimiter has been read, i.e. if the document is considered complete. If this is still
     * false when you run out of input, your document was incomplete.
     *
     * @return true if the parser is in its end state, false otherwise
     */
    public boolean isFinished() {
        return state == State.EPILOGUE;
    }
}
