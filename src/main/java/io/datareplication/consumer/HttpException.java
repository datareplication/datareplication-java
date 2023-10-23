package io.datareplication.consumer;

import io.datareplication.model.Url;
import lombok.EqualsAndHashCode;
import lombok.NonNull;

public class HttpException extends ConsumerException {
    private HttpException(final String message) {
        super(message);
    }

    private HttpException(final String message, final Throwable cause) {
        super(message, cause);
    }

    @EqualsAndHashCode(callSuper = false)
    public static final class InvalidUrl extends HttpException {
        private final Url url;

        public InvalidUrl(@NonNull final Url url, @NonNull final Throwable cause) {
            super(String.format("invalid URL '%s'", url.value()), cause);
            this.url = url;
        }
    }

    @EqualsAndHashCode(callSuper = false)
    public static final class ClientError extends HttpException {
        // TODO: it might be nice to have the response body (or at least the first n bytes/characters)
        private final int statusCode;

        public ClientError(int statusCode) {
            super(String.format("HTTP %s response", statusCode));
            this.statusCode = statusCode;
        }
    }

    @EqualsAndHashCode(callSuper = false)
    public static final class ServerError extends HttpException {
        // TODO: it might be nice to have the response body (or at least the first n bytes/characters)
        private final int statusCode;

        public ServerError(int statusCode) {
            super(String.format("HTTP %s response", statusCode));
            this.statusCode = statusCode;
        }
    }

    @EqualsAndHashCode(callSuper = false)
    public static final class NetworkError extends HttpException {
        public NetworkError(@NonNull final Throwable cause) {
            super(cause.getMessage(), cause);
        }
    }
}
