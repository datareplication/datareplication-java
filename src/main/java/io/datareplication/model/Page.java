package io.datareplication.model;

import lombok.Value;

import java.util.Collections;
import java.util.List;

@Value
public class Page<PageHeader extends HttpHeaders, EntityHeader extends HttpHeaders> {
    PageHeader header;
    List<Entity<EntityHeader>> entities;

    private Page(PageHeader header, List<Entity<EntityHeader>> entities) {
        this.header = header;
        this.entities = Collections.unmodifiableList(entities);
    }

    public Body toMultipartBody() {
        throw new RuntimeException("not implemented");
    }
}
