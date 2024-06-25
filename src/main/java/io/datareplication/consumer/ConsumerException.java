package io.datareplication.consumer;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;

import java.util.List;

// TODO: docs
public abstract class ConsumerException extends RuntimeException {
    protected ConsumerException(String message) {
        super(message);
    }

    protected ConsumerException(String message, Throwable cause) {
        super(message, cause);
    }

    @Getter
    @EqualsAndHashCode(callSuper = false)
    public static final class CollectedErrors extends ConsumerException {
        @NonNull private final List<@NonNull Throwable> exceptions;

        public CollectedErrors(@NonNull List<@NonNull Throwable> exceptions) {
            super(buildMessage(exceptions));
            this.exceptions = List.copyOf(exceptions);
        }

        private static String buildMessage(List<Throwable> exceptions) {
            final var bld = new StringBuilder("Multiple exceptions occurred:")
                .append(System.lineSeparator());
            for (var exc : exceptions) {
                bld
                    .append(" * ")
                    .append(exc.getClass().getName())
                    .append(": ")
                    .append(exc.getMessage())
                    .append(System.lineSeparator());

                var nextCause = exc.getCause();
                while (nextCause != null) {
                    bld
                        .append("   caused by: ")
                        .append(nextCause.getClass().getName())
                        .append(": ")
                        .append(nextCause.getMessage())
                        .append(System.lineSeparator());
                    nextCause = nextCause.getCause();
                }
            }
            return bld.toString();
        }
    }
}
