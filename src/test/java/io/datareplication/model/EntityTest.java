package io.datareplication.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EntityTest {
    @Test
    void shouldCreateEntityWithoutUserData() {
        final Entity<HttpHeaders> entity = new Entity<>(HttpHeaders.EMPTY, Body.fromUtf8("abc"));

        assertThat(entity.userData()).isEmpty();
    }

    @Test
    void shouldReturnHeadersFromHeaderAndBody() {
        final Entity<HttpHeaders> entity = new Entity<>(
            HttpHeaders.of(HttpHeader.of("h1", "v1"),
                           HttpHeader.of("h2", "v2")),
            Body.fromUtf8("abc", ContentType.of("text/plain")));

        final HttpHeaders headers = entity.toHttpHeaders();

        assertThat(headers).containsExactlyInAnyOrder(
            HttpHeader.of("h1", "v1"),
            HttpHeader.of("h2", "v2"),
            HttpHeader.contentType(ContentType.of("text/plain")),
            HttpHeader.contentLength(3)
        );
    }
}
