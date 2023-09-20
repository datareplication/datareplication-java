package io.datareplication.model;

import lombok.NonNull;
import lombok.Value;

import java.util.Collections;
import java.util.List;

@Value
public class Page<PageHeader extends HttpHeaders, EntityHeader extends HttpHeaders> {
    @NonNull
    PageHeader header;
    @NonNull
    List<@NonNull Entity<@NonNull EntityHeader>> entities;

    private Page(@NonNull PageHeader header, @NonNull List<@NonNull Entity<@NonNull EntityHeader>> entities) {
        this.header = header;
        this.entities = Collections.unmodifiableList(entities);
    }

    public @NonNull Body toMultipartBody() {
        throw new RuntimeException("not implemented");
    }
}
