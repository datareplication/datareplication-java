package io.datareplication.internal.multipart;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.Value;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
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

    private static final ByteBuffer LF = ByteBuffer.wrap("\n".getBytes(StandardCharsets.US_ASCII));
    private static final ByteBuffer CRLF = ByteBuffer.wrap("\r\n".getBytes(StandardCharsets.US_ASCII));

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

    static Parser scan(ByteBuffer needle) {
        return (ByteBuffer input, int start) -> scan(input, start, needle);
    }

    static Parser scanEol() {
        return Combinators::scanEol;
    }

    static Parser either(Parser a, Parser b) {
        return (ByteBuffer input, int start) -> {
            Optional<Pos> aResult = a.parse(input, start);
            if (aResult.isPresent()) {
                return aResult;
            } else {
                return b.parse(input, start);
            }
        };
    }

    static Parser eol() {
        return either(tag(CRLF), tag(LF));
    }

    /*static Parser discard(Parser inner) {
        return (ByteBuffer input, int start) -> inner.parse(input, start).or(() -> Optional.of(new Pos(start, start)));
    }*/

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

    static Optional<Pos> scanEol(ByteBuffer input, int start) throws RequestInput {
        Optional<Pos> crlf = scan(input, start, CRLF);
        if (crlf.isPresent()) {
            return crlf;
        } else {
            return scan(input, start, LF);
        }
    }

    static Optional<Pos> scan(ByteBuffer input, int start, ByteBuffer needle) throws RequestInput {
        int needlePos = 0;
        for (int i = start; i < input.capacity(); i++) {
            byte inputByte = input.get(i);
            byte needleByte = needle.get(needlePos);
            if (inputByte == needleByte) {
                needlePos++;
            } else {
                needlePos = 0;
            }

            if (needlePos == needle.capacity()) {
                // we've matched all bytes in the needle, so we're done and can return
                return Optional.of(new Pos(i - needle.capacity() + 1, i + 1));
            }
        }

        // we've reached the end of our input
        if (needlePos > 0) {
            // we've partially but not fully matched the needle, so we ask for more input
            throw new RequestInput();
        } else {
            // we're not in the middle of a match, so we know it's not here
            return Optional.empty();
        }
    }
}
