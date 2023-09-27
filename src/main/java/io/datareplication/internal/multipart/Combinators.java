package io.datareplication.internal.multipart;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.Value;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
class Combinators {
    private static final ByteBuffer CRLF = ByteBuffer.wrap("\r\n".getBytes(StandardCharsets.US_ASCII));
    private static final ByteBuffer LF = ByteBuffer.wrap("\n".getBytes(StandardCharsets.US_ASCII));

    @FunctionalInterface
    interface Parser {
        Optional<Pos> parse(ByteBuffer input, int start);
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

    static Parser seq(Parser first, Parser second) {
        return (ByteBuffer input, int start) -> first
            .parse(input, start)
            .flatMap(pos1 -> second
                 .parse(input, pos1.end)
                 .map(pos2 -> new Pos(pos1.start, pos2.end)));
    }

    static Parser either(Parser first, Parser second) {
        return (ByteBuffer input, int start) -> first
            .parse(input, start)
            .or(() -> second.parse(input, start));
    }

    static Parser scan(Parser parser) {
        return (ByteBuffer input, int start) -> {
            for (int i = start; i < input.limit(); i++) {
                Optional<Pos> result = parser.parse(input, i);
                if (result.isPresent()) {
                    return result;
                }
            }
            return Optional.empty();
        };
    }

    static Parser eol() {
        return either(tag(CRLF), tag(LF));
    }
}
