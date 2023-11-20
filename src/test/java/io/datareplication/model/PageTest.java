package io.datareplication.model;

import io.datareplication.producer.snapshot.PageIdProvider;
import io.datareplication.producer.snapshot.UUIDPageIdProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PageTest {
    private static final Entity<HttpHeaders> ENTITY_1 = new Entity<>(HttpHeaders.EMPTY, Body.fromUtf8("1"));
    private static final Entity<HttpHeaders> ENTITY_2 = new Entity<>(HttpHeaders.EMPTY, Body.fromUtf8("2"));
    private static final Entity<HttpHeaders> ENTITY_3 = new Entity<>(HttpHeaders.EMPTY, Body.fromUtf8("3"));

    @Test
    void shouldMakeEntitiesListUnmodifiable() {

        final ArrayList<Entity<HttpHeaders>> original = new ArrayList<>();
        original.add(ENTITY_1);
        original.add(ENTITY_2);
        final ArrayList<Entity<HttpHeaders>> entities = new ArrayList<>(original);

        final Page<HttpHeaders, HttpHeaders> page
            = new Page<>(HttpHeaders.EMPTY, "boundary", entities);

        assertThat(page.entities()).containsExactlyElementsOf(original);
        assertThatThrownBy(() -> page.entities().add(ENTITY_3))
            .isInstanceOf(UnsupportedOperationException.class);
        entities.add(ENTITY_3);
        assertThat(page.entities()).containsExactlyElementsOf(original);
    }
}
