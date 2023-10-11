package io.datareplication.model.feed;

/**
 * A feed entity's operation type. These correspond to HTTP verbs. They have no impact on how an entity is actually
 * processed by this library so any semantic interpretation happens outside the consumer. As such, the descriptions of
 * behaviour for the different types are only informational: the exact semantics of how to use these values and how they
 * have to be interpreted by consumers are left up to the provider of the feed to define.
 */
public enum OperationType {
    /**
     * This entity represents a complete replacement of some resource. The entity body is expected to contain a
     * reference to the resource and all its data. The state of the resource after processing this entity should not
     * depend on its previous state.
     */
    PUT,
    /**
     * This entity represents the deletion of a resource. After processing this entity, the resource should not exist
     * regardless of its previous state.
     */
    DELETE,
    /**
     * This entity represents a partial update of some resource. The entity body should contain a reference to the
     * resource and the subset of the data that should be changed. PATCH updates may still be idempotent but don't
     * necessarily have to be.
     */
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
