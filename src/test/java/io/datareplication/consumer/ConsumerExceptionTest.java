package io.datareplication.consumer;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ConsumerExceptionCollectedErrorsTest {
    private static final String NEWLINE = System.lineSeparator();

    @Test
    void shouldBuildMessageFromMultipleExceptionsWithCauses() {
        final var collectedErrors = new ConsumerException.CollectedErrors(List.of(
            new RuntimeException("exception 1", new IOException("cause 1")),
            new IllegalArgumentException("123"),
            new IllegalStateException("exception 2", new RuntimeException("cause 2", new Exception("cause 3")))
        ));

        assertThat(collectedErrors.getMessage()).isEqualTo(
            "Multiple exceptions occurred:" + NEWLINE +
                " * java.lang.RuntimeException: exception 1" + NEWLINE +
                "   caused by: java.io.IOException: cause 1" + NEWLINE +
                " * java.lang.IllegalArgumentException: 123" + NEWLINE +
                " * java.lang.IllegalStateException: exception 2" + NEWLINE +
                "   caused by: java.lang.RuntimeException: cause 2" + NEWLINE +
                "   caused by: java.lang.Exception: cause 3" + NEWLINE
        );
    }
}
