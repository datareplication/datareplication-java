package io.datareplication.internal.multipart;

import lombok.EqualsAndHashCode;

import java.nio.charset.Charset;

public abstract class MultipartException extends RuntimeException {
    private MultipartException(String message) {
        super(message);
    }

    @EqualsAndHashCode(callSuper = false)
    public static final class InvalidDelimiter extends MultipartException {
        private final long offset;

        public InvalidDelimiter(long offset) {
            super(String.format("expected either '--' or newline following multipart dash delimiter at %d", offset));
            this.offset = offset;
        }
    }

    @EqualsAndHashCode(callSuper = false)
    public static final class UndecodableHeader extends MultipartException {
        private final Charset charset;
        private final long offset;

        public UndecodableHeader(Charset charset, long offset) {
            super(String.format("header bytes couldn't be decoded with '%s' at %d", charset, offset));
            this.charset = charset;
            this.offset = offset;
        }
    }

    @EqualsAndHashCode(callSuper = false)
    public static final class InvalidHeader extends MultipartException {
        private final String header;
        private final long offset;

        public InvalidHeader(String header, long offset) {
            super(String.format("invalid header line '%s' (missing ':') at %d", header, offset));
            this.header = header;
            this.offset = offset;
        }
    }

    @EqualsAndHashCode(callSuper = false)
    public static final class UnexpectedEndOfInput extends MultipartException {
        private final long offset;

        public UnexpectedEndOfInput(long offset) {
            super(String.format("unexpected end of input at %d", offset));
            this.offset = offset;
        }
    }
}
