package io.datareplication.internal.multipart;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.Value;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
class Combinators {
    @Value
    static class Pos {
        int start;
        int end;
    }

    private static final ByteBuffer LF = ByteBuffer.wrap("\n".getBytes(StandardCharsets.US_ASCII));
    private static final ByteBuffer CRLF = ByteBuffer.wrap("\r\n".getBytes(StandardCharsets.US_ASCII));

    static Optional<Pos> scanEol(ByteBuffer input, int start) throws RequestInput {
        Optional<Pos> lf = scan(input, start, LF);
        if (lf.isPresent()) {
            return lf;
        } else {
            return scan(input, start, CRLF);
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
