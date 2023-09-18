package io.datareplication.model;

import lombok.Value;

import java.util.Collections;
import java.util.List;

@Value(staticConstructor = "of")
public class Page<Header extends HttpHeaders, Entity> {
    Header header;
    List<Entity> entities;

    private Page(Header header, List<Entity> entities) {
        this.header = header;
        this.entities = Collections.unmodifiableList(entities);
    }


}
