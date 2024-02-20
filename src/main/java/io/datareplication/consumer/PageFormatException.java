package io.datareplication.consumer;

import io.datareplication.model.HttpHeaders;
import lombok.EqualsAndHashCode;
import lombok.NonNull;

// TODO: docs
public class PageFormatException extends ConsumerException {
    private PageFormatException(@NonNull final String message) {
        super(message);
    }

    private PageFormatException(@NonNull final String message, final Throwable cause) {
        super(message, cause);
    }

    @EqualsAndHashCode(callSuper = false)
    public static final class MissingContentTypeHeader extends PageFormatException {
        private final HttpHeaders httpHeaders;

        public MissingContentTypeHeader(@NonNull final HttpHeaders httpHeaders) {
            super(String.format("Content-Type header is missing from HTTP response: '%s'", httpHeaders));
            this.httpHeaders = httpHeaders;
        }
    }

    @EqualsAndHashCode(callSuper = false)
    public static final class MissingLastModifiedHeader extends PageFormatException {
        private final HttpHeaders httpHeaders;

        public MissingLastModifiedHeader(@NonNull final HttpHeaders httpHeaders) {
            super(String.format("Last-Modified header is missing from HTTP response: '%s'", httpHeaders));
            this.httpHeaders = httpHeaders;
        }
    }

    @EqualsAndHashCode(callSuper = false)
    public static final class InvalidLastModifiedHeader extends PageFormatException {
        public InvalidLastModifiedHeader(@NonNull final String lastModified,
                                         final Throwable cause) {
            super(String.format("Last-Modified header is invalid: '%s'", lastModified), cause);
        }
    }

    @EqualsAndHashCode(callSuper = false)
    public static final class MissingLinkHeader extends PageFormatException {
        private final HttpHeaders httpHeaders;
        private final String rel;

        public MissingLinkHeader(@NonNull final HttpHeaders httpHeaders) {
            super(String.format("Link header is missing from HTTP response: '%s'", httpHeaders));
            this.httpHeaders = httpHeaders;
            this.rel = null;
        }

        public MissingLinkHeader(@NonNull final HttpHeaders httpHeaders, @NonNull final String rel) {
            super(String.format("LINK; rel=%s header is missing from HTTP response: '%s'", rel, httpHeaders));
            this.httpHeaders = httpHeaders;
            this.rel = rel;
        }
    }

    @EqualsAndHashCode(callSuper = false)
    public static final class UnparseableContentTypeHeader extends PageFormatException {
        private final String contentTypeHeader;

        public UnparseableContentTypeHeader(@NonNull final String contentTypeHeader) {
            super(String.format("unparseable Content-Type header: '%s'", contentTypeHeader));
            this.contentTypeHeader = contentTypeHeader;
        }

        public UnparseableContentTypeHeader(@NonNull final String contentTypeHeader,
                                            final Throwable cause) {
            super(String.format("unparseable Content-Type header: '%s'", contentTypeHeader), cause);
            this.contentTypeHeader = contentTypeHeader;
        }
    }

    @EqualsAndHashCode(callSuper = false)
    public static final class NoBoundaryInContentTypeHeader extends PageFormatException {
        private final String contentTypeHeader;

        public NoBoundaryInContentTypeHeader(@NonNull final String contentTypeHeader) {
            super(String.format("required boundary is missing from Content-Type header: '%s'", contentTypeHeader));
            this.contentTypeHeader = contentTypeHeader;
        }
    }

    @EqualsAndHashCode(callSuper = false)
    public static final class InvalidContentType extends PageFormatException {
        private final String contentType;

        public InvalidContentType(@NonNull final String contentType) {
            super(String.format("unexpected content type: expected multipart/*, found: '%s'", contentType));
            this.contentType = contentType;
        }
    }

    @EqualsAndHashCode(callSuper = false)
    public static final class InvalidMultipart extends PageFormatException {
        private final Throwable cause;

        public InvalidMultipart(final Throwable cause) {
            super(String.format("invalid multipart document: '%s'", cause));
            this.cause = cause;
        }
    }

    @EqualsAndHashCode(callSuper = false)
    public static final class MissingLastModifiedHeaderInEntity extends PageFormatException {
        private final int index;

        public MissingLastModifiedHeaderInEntity(final int index) {
            super(String.format("Last-Modified header is missing from entity at index %s", index));
            this.index = index;
        }
    }

    @EqualsAndHashCode(callSuper = false)
    public static final class InvalidLastModifiedHeaderInEntity extends PageFormatException {
        private final int index;

        public InvalidLastModifiedHeaderInEntity(final int index,
                                                 @NonNull final String lastModified,
                                                 final Throwable cause) {
            super(
                String.format(
                    "unparseable Last-Modified header from entity at index %s: '%s'",
                    index,
                    lastModified
                ),
                cause);
            this.index = index;
        }
    }

    @EqualsAndHashCode(callSuper = false)
    public static final class MissingContentTypeInEntity extends PageFormatException {
        private final int index;

        public MissingContentTypeInEntity(final int index) {
            super(String.format("Content-Type header is missing from entity at index %s", index));
            this.index = index;
        }
    }

    @EqualsAndHashCode(callSuper = false)
    public static final class MissingContentIdInEntity extends PageFormatException {
        private final int index;

        public MissingContentIdInEntity(final int index) {
            super(String.format("Content-Id header is missing from entity at index %s", index));
            this.index = index;
        }
    }

    @EqualsAndHashCode(callSuper = false)
    public static final class MissingOperationTypeInEntity extends PageFormatException {
        private final int index;

        public MissingOperationTypeInEntity(final int index) {
            super(String.format("Operation-Type header is missing from entity at index %s", index));
            this.index = index;
        }
    }

    @EqualsAndHashCode(callSuper = false)
    public static final class UnparseableOperationTypeInEntity extends PageFormatException {
        private final String contentTypeHeader;
        private final int index;

        public UnparseableOperationTypeInEntity(@NonNull final Integer index,
                                                @NonNull final String contentTypeHeader,
                                                final Throwable cause) {
            super(
                String.format(
                    "unparseable Operation-Type header from entity at index %s: '%s'",
                    index,
                    contentTypeHeader
                ),
                cause);
            this.contentTypeHeader = contentTypeHeader;
            this.index = index;
        }
    }
}
