package io.datareplication.consumer;

import lombok.EqualsAndHashCode;
import lombok.NonNull;

public class PageFormatException extends ConsumerException {
    private PageFormatException(final String message) {
        super(message);
    }

    private PageFormatException(final String message, final Throwable cause) {
        super(message, cause);
    }

    @EqualsAndHashCode(callSuper = false)
    public static final class MissingContentTypeHeader extends PageFormatException {
        public MissingContentTypeHeader() {
            super("Content-Type header is missing from HTTP response");
        }
    }

    @EqualsAndHashCode(callSuper = false)
    public static final class UnparseableContentTypeHeader extends PageFormatException {
        private final String contentTypeHeader;

        public UnparseableContentTypeHeader(@NonNull String contentTypeHeader) {
            super(String.format("unparseable Content-Type header: '%s'", contentTypeHeader));
            this.contentTypeHeader = contentTypeHeader;
        }

        public UnparseableContentTypeHeader(@NonNull String contentTypeHeader, @NonNull Throwable cause) {
            super(String.format("unparseable Content-Type header: '%s'", contentTypeHeader), cause);
            this.contentTypeHeader = contentTypeHeader;
        }
    }

    @EqualsAndHashCode(callSuper = false)
    public static final class NoBoundaryInContentTypeHeader extends PageFormatException {
        private final String contentTypeHeader;

        public NoBoundaryInContentTypeHeader(@NonNull String contentTypeHeader) {
            super(String.format("required boundary is missing from Content-Type header: '%s'", contentTypeHeader));
            this.contentTypeHeader = contentTypeHeader;
        }
    }

    @EqualsAndHashCode(callSuper = false)
    public static final class InvalidContentType extends PageFormatException {
        private final String contentType;

        public InvalidContentType(@NonNull String contentType) {
            super(String.format("unexpected content type: expected multipart/*, found %s", contentType));
            this.contentType = contentType;
        }
    }

    @EqualsAndHashCode(callSuper = false)
    public static final class InvalidMultipart extends PageFormatException {
        private final Throwable cause;

        public InvalidMultipart(@NonNull Throwable cause) {
            super(String.format("invalid multipart document: %s", cause));
            this.cause = cause;
        }
    }
}
