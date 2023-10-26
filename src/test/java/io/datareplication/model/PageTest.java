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

    private PageIdProvider pageIdProvider;

    @BeforeEach
    void setUp() {
        pageIdProvider = new UUIDPageIdProvider();
    }

    @Test
    void shouldMakeEntitiesListUnmodifiable() {

        final ArrayList<Entity<HttpHeaders>> original = new ArrayList<>();
        original.add(ENTITY_1);
        original.add(ENTITY_2);
        final ArrayList<Entity<HttpHeaders>> entities = new ArrayList<>(original);

        final Page<HttpHeaders, HttpHeaders> page
            = new Page<>(HttpHeaders.EMPTY, entities, pageIdProvider.newPageId().value());

        assertThat(page.entities()).containsExactlyElementsOf(original);
        assertThatThrownBy(() -> page.entities().add(ENTITY_3))
            .isInstanceOf(UnsupportedOperationException.class);
        entities.add(ENTITY_3);
        assertThat(page.entities()).containsExactlyElementsOf(original);
    }

    @Test
    void shouldPickARandomBoundaryFromARandomUUID() {
        pageIdProvider = new UUIDPageIdProvider();
        final Page<HttpHeaders, HttpHeaders> page
            = new Page<>(HttpHeaders.EMPTY, Collections.emptyList(), pageIdProvider.newPageId().value());

        assertThat(page.boundary()).matches("_---_[a-f0-9-]{36}");
        final UUID uuidPart = UUID.fromString(page.boundary().substring(5));
        assertThat(uuidPart.version()).isEqualTo(4);
    }
}
