package io.datareplication.consumer;

import io.datareplication.model.HttpHeaders;
import io.datareplication.model.Url;
import io.datareplication.model.feed.Link;
import lombok.EqualsAndHashCode;
import lombok.NonNull;

import java.util.Map;

// TODO: docs
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
    public static final class MissingLastModifiedHeader extends PageFormatException {

        public MissingLastModifiedHeader() {
            super("Last-Modified header is missing from HTTP response");
        }
    }

    @EqualsAndHashCode(callSuper = false)
    public static final class MissingLinkHeader extends PageFormatException {

        public MissingLinkHeader() {
            super("Link header is missing from HTTP response");
        }
    }

    @EqualsAndHashCode(callSuper = false)
    public static final class MissingSelfLinkHeader extends PageFormatException {
        private final HttpHeaders links;

        public MissingSelfLinkHeader(HttpHeaders links) {
            super(String.format("LINK; rel=self header is missing from HTTP response: %s", links));
            this.links = links;
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

    @EqualsAndHashCode(callSuper = false)
    public static final class MissingLastModifiedHeaderInEntity extends PageFormatException {
        private final int index;

        public MissingLastModifiedHeaderInEntity(int index) {
            super(String.format("Last-Modified header is missing from entity at index %s", index));
            this.index = index;
        }
    }

    @EqualsAndHashCode(callSuper = false)
    public static final class MissingContentTypeInEntity extends PageFormatException {
        private final int index;

        public MissingContentTypeInEntity(int index) {
            super(String.format("Content-Type header is missing from entity at index %s", index));
            this.index = index;
        }
    }

    @EqualsAndHashCode(callSuper = false)
    public static final class MissingContentIdInEntity extends PageFormatException {
        private final int index;

        public MissingContentIdInEntity(int index) {
            super(String.format("Content-Id header is missing from entity at index %s", index));
            this.index = index;
        }
    }

    @EqualsAndHashCode(callSuper = false)
    public static final class MissingOperationTypeInEntity extends PageFormatException {
        private final int index;

        public MissingOperationTypeInEntity(int index) {
            super(String.format("Operation-Type header is missing from entity at index %s", index));
            this.index = index;
        }
    }
}
