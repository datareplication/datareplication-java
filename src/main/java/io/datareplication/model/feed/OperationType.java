package io.datareplication.model.feed;

public enum OperationType {
    PUT,
    DELETE;

    @Override
    public String toString() {
        switch (this) {
            case PUT:
                return "PUT";
            case DELETE:
                return "DELETE";
            default:
                throw new RuntimeException("unknown value");
        }
    }
}
