package io.datareplication.model.feed;

public enum OperationType {
    PUT,
    DELETE,
    PATCH;

    @Override
    public String toString() {
        switch (this) {
            case PUT:
                return "PUT";
            case DELETE:
                return "DELETE";
            case PATCH:
                return "PATCH";
            default:
                throw new RuntimeException("unknown value");
        }
    }
}
