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

public class MultipartParser {
    @Value
    public static class Result {
        @NonNull Elem elem;
        int consumedBytes;

        private static Result data(ByteBuffer data) {
            return new Result(new Elem.Data(data), data.limit());
        }
    }

    private enum State {
        Preamble,
        PartBegin,
        Headers,
        Data,
        Epilogue,
    }

    private State state;
    private long offset;
    private final ByteBuffer dashBoundary;
    private final CharsetDecoder headerDecoder;

    private static final ByteBuffer CLOSE_DELIMITER = ByteBuffer.wrap("--".getBytes(StandardCharsets.US_ASCII));

    public MultipartParser(@NonNull ByteBuffer boundary, @NonNull Charset headerCharset) {
        state = State.Preamble;
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

    public MultipartParser(@NonNull ByteBuffer boundary) {
        this(boundary, StandardCharsets.UTF_8);
    }

    public @NonNull Result parse(@NonNull ByteBuffer input) throws RequestInput, MultipartException {
        Result result = parseInternal(input);
        offset += result.consumedBytes;
        return result;
    }

    private Result parseInternal(final ByteBuffer input) {
        switch (state) {
            case Preamble:
                return Combinators
                        .tag(dashBoundary)
                        .parse(input, 0)
                        .map(pos -> {
                            state = State.PartBegin;
                            return new Result(Elem.Continue.INSTANCE, pos.end());
                        })
                        .or(() -> Combinators
                                .scan(Combinators.eol())
                                .parse(input, 0)
                                .map(pos -> new Result(Elem.Continue.INSTANCE, pos.end())))
                        .orElseGet(() -> new Result(Elem.Continue.INSTANCE, input.limit()));
            case PartBegin:
                return Combinators
                        .eol()
                        .parse(input, 0)
                        .map(pos -> {
                            state = State.Headers;
                            return new Result(Elem.PartBegin.INSTANCE, pos.end());
                        })
                        .or(() -> Combinators
                                .tag(CLOSE_DELIMITER)
                                .parse(input, 0)
                                .map(pos -> {
                                    state = State.Epilogue;
                                    return new Result(Elem.Continue.INSTANCE, pos.end());
                                }))
                        .orElseThrow(() -> new MultipartException.InvalidDelimiter(offset));
            case Headers:
                final Combinators.Pos eol = Combinators
                        .scan(Combinators.eol())
                        .parse(input, 0)
                        .orElseThrow(RequestInput::new);
                if (eol.start() == 0) {
                    // immediate newline, go to body
                    state = State.Data;
                    return new Result(Elem.DataBegin.INSTANCE, eol.end());
                } else {
                    final ByteBuffer headerLine = input.slice().limit(eol.start());
                    return new Result(parseHeader(headerLine), eol.end());
                }
            case Data:
                return Combinators
                        .seq(Combinators.seq(Combinators.eol(), Combinators.eol()),
                             Combinators.tag(dashBoundary))
                        .parse(input, 0)
                        .map(pos -> {
                            state = State.PartBegin;
                            return new Result(Elem.PartEnd.INSTANCE, pos.end());
                        })
                        .or(() -> Combinators
                                .scan(Combinators.eol())
                                .parse(input, 0)
                                .flatMap(pos -> pos.start() == 0
                                        ? Combinators.scan(Combinators.eol()).parse(input, pos.end())
                                        : Optional.of(pos))
                                .map(pos -> Result.data(input.slice().limit(pos.start()))))
                        .orElseGet(() -> Result.data(input.slice()));
            case Epilogue:
                return new Result(Elem.Continue.INSTANCE, input.limit());
            default:
                throw new IllegalStateException(String.format("unknown state %s; bug in parser?", state));
        }
    }

    private Elem.Header parseHeader(ByteBuffer bytes) {
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
        return new Elem.Header(name, value);
    }

    public boolean isFinished() {
        return state == State.Epilogue;
    }
}
