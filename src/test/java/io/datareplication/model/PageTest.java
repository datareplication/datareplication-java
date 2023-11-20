package io.datareplication.model;

import lombok.NonNull;
import lombok.Value;
import io.datareplication.producer.snapshot.PageIdProvider;
import io.datareplication.producer.snapshot.UUIDPageIdProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PageTest {
    private static final Entity<HttpHeaders> ENTITY_1 = new Entity<>(HttpHeaders.EMPTY, Body.fromUtf8("1"));
    private static final Entity<HttpHeaders> ENTITY_2 = new Entity<>(HttpHeaders.EMPTY, Body.fromUtf8("2"));
    private static final Entity<HttpHeaders> ENTITY_3 = new Entity<>(HttpHeaders.EMPTY, Body.fromUtf8("3"));

    @Value
    private static class TestHeaders implements ToHttpHeaders {
        String header1;
        int header2;

        @Override
        public @NonNull HttpHeaders toHttpHeaders() {
            return HttpHeaders.of(
                HttpHeader.of("header1", header1),
                HttpHeader.of("header2", Integer.toString(header2))
            );
        }
    }

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

    // The order of the headers is unfortunately not guaranteed because they come from a HashMap. Let's just hope the
    // ordering is sufficiently stable that these tests don't turn into a problem?
    @Test
    void toMultipartBody_shouldBuildMultipartBodyFromOneEntity() throws IOException {
        final var page = new Page<>(
            HttpHeaders.EMPTY,
            "_---_boundary-string",
            List.of(
                new Entity<>(
                    new TestHeaders("value1", 15),
                    Body.fromUtf8("test entity body", ContentType.of("text/x-vnd-test")))
            ));

        final var result = page.toMultipartBody();

        assertThat(result.contentType())
            .isEqualTo(ContentType.of("multipart/mixed; boundary=\"_---_boundary-string\""));
        assertThat(result.toUtf8()).isEqualTo(
            "--_---_boundary-string\r\n"
                + "Content-Length: 16\r\n"
                + "Content-Type: text/x-vnd-test\r\n"
                + "header2: 15\r\n"
                + "header1: value1\r\n"
                + "\r\n"
                + "test entity body\r\n"
                + "--_---_boundary-string--"
        );
        assertThat(result.contentLength()).isEqualTo(149);
    }

    @Test
    void toMultipartBody_shouldBuildMultipartBodyFromMultipleEntities() throws IOException {
        final var page = new Page<>(
            HttpHeaders.EMPTY,
            "cool cool boundary ;)",
            List.of(
                new Entity<>(
                    new TestHeaders("entity 1", 66),
                    Body.fromUtf8("test \nentity \n1\n", ContentType.of("text/x-vnd-test-1"))),
                new Entity<>(
                    new TestHeaders("entity 2", 67),
                    Body.fromUtf8("test \nentity \n2", ContentType.of("text/x-vnd-test-2"))),
                new Entity<>(
                    new TestHeaders("entity 3", 68),
                    Body.fromUtf8("test \nentity \n3", ContentType.of("text/x-vnd-test-3")))
            ));

        final var result = page.toMultipartBody();

        assertThat(result.contentType())
            .isEqualTo(ContentType.of("multipart/mixed; boundary=\"cool cool boundary ;)\""));
        assertThat(result.toUtf8()).isEqualTo(
            "--cool cool boundary ;)\r\n"
                + "Content-Length: 16\r\n"
                + "Content-Type: text/x-vnd-test-1\r\n"
                + "header2: 66\r\n"
                + "header1: entity 1\r\n"
                + "\r\n"
                + "test \n"
                + "entity \n"
                + "1\n"
                + "\r\n"
                + "--cool cool boundary ;)\r\n"
                + "Content-Length: 15\r\n"
                + "Content-Type: text/x-vnd-test-2\r\n"
                + "header2: 67\r\n"
                + "header1: entity 2\r\n"
                + "\r\n"
                + "test \n"
                + "entity \n"
                + "2\r\n"
                + "--cool cool boundary ;)\r\n"
                + "Content-Length: 15\r\n"
                + "Content-Type: text/x-vnd-test-3\r\n"
                + "header2: 68\r\n"
                + "header1: entity 3\r\n"
                + "\r\n"
                + "test \n"
                + "entity \n"
                + "3\r\n"
                + "--cool cool boundary ;)--"
        );
        assertThat(result.contentLength()).isEqualTo(413);
    }

    // going by the grammar in the RFC, a multipart document has to have at least one part, but we can support empty
    // ones easy enough
    @Test
    void toMultipartBody_shouldBuildMultipartBodyFromZeroEntities() throws IOException {
        final var page = new Page<>(
            HttpHeaders.EMPTY,
            "the space between worlds",
            List.of());

        final var result = page.toMultipartBody();

        assertThat(result.contentType())
            .isEqualTo(ContentType.of("multipart/mixed; boundary=\"the space between worlds\""));
        assertThat(result.contentLength()).isEqualTo(28);
        assertThat(result.toUtf8()).isEqualTo("--the space between worlds--");
    }

    @Test
    void toMultipartBody_fromEntityWithoutHeaders() throws IOException {
        final var page = new Page<>(
            HttpHeaders.EMPTY,
            "bnd",
            List.of(
                new Entity<>(
                    HttpHeaders.EMPTY,
                    Body.fromUtf8("body", ContentType.of("application/xml")))
            ));

        final var result = page.toMultipartBody();

        assertThat(result.contentType())
            .isEqualTo(ContentType.of("multipart/mixed; boundary=\"bnd\""));
        assertThat(result.toUtf8()).isEqualTo(
            "--bnd\r\n"
                + "Content-Length: 4\r\n"
                + "Content-Type: application/xml\r\n"
                + "\r\n"
                + "body\r\n"
                + "--bnd--"
        );
        assertThat(result.contentLength()).isEqualTo(72);
    }

    @Test
    void toMultipartBody_fromEntityWithEmptyBody() throws IOException {
        final var page = new Page<>(
            HttpHeaders.EMPTY,
            " boundary string ",
            List.of(
                new Entity<>(
                    HttpHeaders.EMPTY,
                    Body.fromBytes(new byte[0], ContentType.of("application/x-vnd-nothing")))
            ));

        final var result = page.toMultipartBody();

        assertThat(result.contentType())
            .isEqualTo(ContentType.of("multipart/mixed; boundary=\" boundary string \""));
        assertThat(result.toUtf8()).isEqualTo(
            "-- boundary string \r\n"
                + "Content-Length: 0\r\n"
                + "Content-Type: application/x-vnd-nothing\r\n"
                + "\r\n"
                + "\r\n"
                + "-- boundary string --"
        );
        assertThat(result.contentLength()).isEqualTo(106);
    }
}
