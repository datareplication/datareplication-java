package io.datareplication.internal.multipart;

import lombok.NonNull;
import lombok.Value;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

public class MultipartParser {

    @Value
    public static class Result {
        @NonNull Elem elem;
        @NonNull int consumedBytes;
    }

    private enum State {
        Preamble,
        PartBegin,
        Headers,
        Data,
        Epilogue,
    }

    private State state;
    private final ByteBuffer dashBoundary;
    private final Charset headerCharset;

    private static final ByteBuffer CLOSE_DELIMITER = ByteBuffer.wrap("--".getBytes(StandardCharsets.US_ASCII));

    public MultipartParser(@NonNull ByteBuffer boundary, @NonNull Charset headerCharset) {
        state = State.Preamble;
        this.headerCharset = headerCharset;
        dashBoundary = ByteBuffer.allocate(boundary.capacity() + 2);
        dashBoundary.put((byte) '-');
        dashBoundary.put((byte) '-');
        dashBoundary.put(boundary);
    }

    public MultipartParser(@NonNull ByteBuffer boundary) {
        this(boundary, StandardCharsets.UTF_8);
    }

    // TODO: real errors
    public Result parse(@NonNull ByteBuffer input) throws RequestInput {
        switch (state) {
            case Preamble:
                final Optional<Combinators.Pos> maybeBoundary = Combinators.tag(dashBoundary).parse(input, 0);
                if (maybeBoundary.isPresent()) {
                    state = State.PartBegin;
                    return new Result(Elem.Continue.INSTANCE, maybeBoundary.get().end());
                } else {
                    return Combinators
                        .scanEol()
                        .parse(input, 0)
                        .map(pos -> new Result(Elem.Continue.INSTANCE, pos.end()))
                        .orElseGet(() -> new Result(Elem.Continue.INSTANCE, input.limit()));
                }
            case PartBegin:
                final Optional<Combinators.Pos> maybeEol = Combinators.eol().parse(input, 0);
                if (maybeEol.isPresent()) {
                    state = State.Headers;
                    return new Result(Elem.PartBegin.INSTANCE, maybeEol.get().end());
                } else {
                    final Optional<Combinators.Pos> maybeClose = Combinators.tag(CLOSE_DELIMITER).parse(input, 0);
                    if (maybeClose.isPresent()) {
                        state = State.Epilogue;
                        return new Result(Elem.Continue.INSTANCE, maybeClose.get().end());
                    }
                }
                throw new RuntimeException("TODO");
            case Headers:
                Optional<Combinators.Pos> maybeLine = Combinators.scanEol().parse(input, 0);
                // TODO: check for too big headers
                if (maybeLine.isEmpty()) {
                    throw new RequestInput();
                }

                final ByteBuffer headerLine = input.slice().limit(maybeLine.get().end());
                String headerString = headerCharset.decode(headerLine).toString();
                if (headerString.isBlank()) {
                    // empty line, go to body
                    //ByteBuffer data = input.slice().position(maybeLine.get().end());
                    state = State.Data;
                    return new Result(Elem.DataBegin.INSTANCE, maybeLine.get().end());
                } else {
                    int idx = headerString.indexOf(':');
                    if (idx == -1) {
                        throw new RuntimeException("TODO");
                    }
                    String name = headerString.substring(0, idx).trim();
                    String value = headerString.substring(idx + 1).trim();
                    return new Result(new Elem.Header(name, value), maybeLine.get().end());
                }
            case Data:
                final Optional<Combinators.Pos> maybeLine2 = Combinators.eol().parse(input, 0);
                if (maybeLine2.isPresent()) {
                    int p = maybeLine2.get().end();
                    final Optional<Combinators.Pos> end = Combinators.seq(Combinators.eol(), Combinators.tag(dashBoundary)).parse(input, p);
                    if (end.isPresent()) {
                        state = State.PartBegin;
                        //final ByteBuffer data = input.slice().limit(maybeLine2.get().start());
                        return new Result(Elem.PartEnd.INSTANCE, end.get().end());
                    } else {
                        final ByteBuffer data = input.slice().limit(maybeLine2.get().end());
                        return new Result(new Elem.Data(data), data.limit());
                    }
                } else {
                    final Optional<Combinators.Pos> maybeLine3 = Combinators.scanEol().parse(input, 0);
                    if (maybeLine3.isPresent()) {
                        final ByteBuffer data = input.slice().limit(maybeLine3.get().start());
                        return new Result(new Elem.Data(data), data.limit());
                    } else {
                        return new Result(new Elem.Data(input.slice()), input.limit());
                    }
                }
            case Epilogue:
                return new Result(Elem.Continue.INSTANCE, input.capacity());
        }

        throw new RuntimeException("not implemented");
    }
}
