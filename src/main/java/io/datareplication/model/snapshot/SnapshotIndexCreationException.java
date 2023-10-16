package io.datareplication.model.snapshot;

public class SnapshotIndexCreationException extends RuntimeException {
    SnapshotIndexCreationException(String message, Throwable t) {
        super(message, t);
    }
}
