package io.datareplication.model;

import lombok.NonNull;
import lombok.Value;

import java.util.Collections;
import java.util.List;

// TODO: have a way to access page ID and (if relevant) snapshot ID via the page or maybe even via the entity?
//       Probably not at entity level because user-created entities for the producer don't have a page ID.
//       Concept: entities are produced by users, pages are produced by the producer impl -> IDs can only be in the
//       pages since they are generated by the impl.
//       Snapshot ID needs to go in the Header classes. For consistency, page ID should also be in there?

@Value
public class Page<PageHeader extends ToHttpHeaders, EntityHeader extends ToHttpHeaders> {
    @NonNull PageHeader header;
    @NonNull List<@NonNull Entity<@NonNull EntityHeader>> entities;

    public Page(@NonNull PageHeader header, @NonNull List<@NonNull Entity<@NonNull EntityHeader>> entities) {
        this.header = header;
        this.entities = Collections.unmodifiableList(entities);
    }

    public @NonNull Body toMultipartBody() {
        throw new RuntimeException("not implemented");
    }
}
