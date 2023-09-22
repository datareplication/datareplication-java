package io.datareplication.internal.multipart;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.Value;

import java.nio.ByteBuffer;
import java.util.Optional;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
class Combinators {
    @FunctionalInterface
    interface Parser {
        Optional<Pos> parse(ByteBuffer input, int start) throws RequestInput;
    }

    @Value
    static class Pos {
        int start;
        int end;
    }

    static Parser tag(ByteBuffer tag) {
        return (ByteBuffer input, int start) -> {
            for (int i = 0; i < tag.capacity(); i++) {
                int inputIdx = start + i;
                if (inputIdx >= input.capacity()) {
                    throw new RequestInput();
                }
                byte needleByte = tag.get(i);
                byte inputByte = input.get(inputIdx);
                if (needleByte != inputByte) {
                    return Optional.empty();
                }
            }
            return Optional.of(new Pos(start, start + tag.capacity()));
        };
    }

    static Parser scanEol() {
        return Combinators::scanEol;
    }

    static Parser eol() {
        return Combinators::eol;
    }

    static Parser seq(Parser a, Parser b) {
        return (ByteBuffer input, int start) -> {
            Optional<Pos> aResult = a.parse(input, start);
            if (aResult.isPresent()) {
                Pos aPos = aResult.get();
                Optional<Pos> bResult = b.parse(input, aPos.end);
                if (bResult.isPresent()) {
                    return Optional.of(new Pos(aPos.start, bResult.get().end));
                }
            }
            return Optional.empty();
        };
    }

    private static Optional<Pos> eol(ByteBuffer input, int start) throws RequestInput {
        byte b = input.get(start);
        if (b == (byte) '\r') {
            if (start == input.limit() - 1) {
                throw new RequestInput();
            }
            if (input.get(start + 1) == (byte) '\n') {
                return Optional.of(new Pos(start, start + 2));
            }
        } else if (b == (byte) '\n') {
            return Optional.of(new Pos(start, start + 1));
        }
        return Optional.empty();
    }

    private static Optional<Pos> scanEol(ByteBuffer input, int start) throws RequestInput {
        for (int i = start; i < input.limit(); i++) {
            final Optional<Pos> maybeEol = eol(input, i);
            if (maybeEol.isPresent()) {
                return maybeEol;
            }
        }
        return Optional.empty();
    }
}
