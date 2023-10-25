package io.datareplication.consumer;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;

import java.util.List;

public class ConsumerException extends RuntimeException {
    ConsumerException(String message) {
        super(message);
    }

    ConsumerException(String message, Throwable cause) {
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

        // TODO: test
        private static String buildMessage(List<Throwable> exceptions) {
            final var bld = new StringBuilder();
            bld.append("Multiple exceptions occurred:");
            bld.append(System.lineSeparator());
            for (var exc: exceptions) {
                bld.append(" * ");
                bld.append(exc.getClass().getName());
                bld.append(": ");
                bld.append(exc.getMessage());
                bld.append(System.lineSeparator());

                var nextCause = exc.getCause();
                while (nextCause != null) {
                    bld.append("   caused by: ");
                    bld.append(nextCause.getClass().getName());
                    bld.append(": ");
                    bld.append(nextCause.getMessage());
                    bld.append(System.lineSeparator());
                    nextCause = nextCause.getCause();
                }
            }
            return bld.toString();
        }
    }
}
