package io.datareplication.internal.multipart;

import io.datareplication.internal.page.MultipartContentType;
import io.datareplication.model.Body;
import io.datareplication.model.ContentType;
import io.datareplication.model.Entity;
import io.datareplication.model.HttpHeader;
import io.datareplication.model.HttpHeaders;
import io.datareplication.model.Page;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class MultipartIntegrationTest {
    private static ByteBuffer utf8(String s) {
        return ByteBuffer.wrap(s.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void shouldParseMultipartBodyFromPage() throws IOException {
        final var page = new Page<>(
            HttpHeaders.EMPTY,
            "_---_boundary-123456",
            List.of(
                new Entity<>(
                    HttpHeaders.of(HttpHeader.of("header", "v1")),
                    Body.fromUtf8("entity1", ContentType.of("text/plain"))),
                new Entity<>(
                    HttpHeaders.of(HttpHeader.of("header", "v2")),
                    Body.fromUtf8("entity2", ContentType.of("text/html"))),
                new Entity<>(
                    HttpHeaders.of(HttpHeader.of("header2", "v3")),
                    Body.fromUtf8("entity3", ContentType.of("audio/mp3")))
            ));

        final var multipartBody = page.toMultipartBody();
        final var parsedContentType = MultipartContentType.parse(multipartBody.contentType().value());
        final var parser = new BufferingMultipartParser(new MultipartParser(utf8(parsedContentType.boundary())));
        final var result = parser.parse(utf8(multipartBody.toUtf8()));
        parser.finish();

        assertThat(result).containsExactly(
            Token.Continue.INSTANCE,
            Token.PartBegin.INSTANCE,
            new Token.Header("Content-Length", "7"),
            new Token.Header("header", "v1"),
            new Token.Header("Content-Type", "text/plain"),
            Token.DataBegin.INSTANCE,
            new Token.Data(utf8("entity1")),
            Token.PartEnd.INSTANCE,
            Token.PartBegin.INSTANCE,
            new Token.Header("Content-Length", "7"),
            new Token.Header("header", "v2"),
            new Token.Header("Content-Type", "text/html"),
            Token.DataBegin.INSTANCE,
            new Token.Data(utf8("entity2")),
            Token.PartEnd.INSTANCE,
            Token.PartBegin.INSTANCE,
            new Token.Header("Content-Length", "7"),
            new Token.Header("Content-Type", "audio/mp3"),
            new Token.Header("header2", "v3"),
            Token.DataBegin.INSTANCE,
            new Token.Data(utf8("entity3")),
            Token.PartEnd.INSTANCE,
            Token.Continue.INSTANCE
        );
    }
}
