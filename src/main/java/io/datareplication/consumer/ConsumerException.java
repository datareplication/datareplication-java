package io.datareplication.consumer;

public class ConsumerException extends RuntimeException {
    ConsumerException(String message) {
        super(message);
    }

    ConsumerException(String message, Throwable cause) {
        super(message, cause);
    }
}
