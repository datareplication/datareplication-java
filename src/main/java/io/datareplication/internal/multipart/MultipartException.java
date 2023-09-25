package io.datareplication.internal.multipart;

public abstract class MultipartException extends RuntimeException {
    private MultipartException(String message) {
        super(message);
    }

    public static class InvalidBoundary extends MultipartException {
        public InvalidBoundary(long offset) {
            super(String.format("expected either '--' or newline following multipart dash boundary at %d", offset));
        }
    }

    public static class InvalidHeader extends MultipartException {
        public InvalidHeader(String header, long offset) {
            super(String.format("invalid header line '%s' (missing ':') at %d", header, offset));
        }
    }
}
