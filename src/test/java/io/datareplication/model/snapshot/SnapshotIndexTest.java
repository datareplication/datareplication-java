package io.datareplication.model.snapshot;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.Schema;
import com.networknt.schema.SchemaLocation;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SchemaRegistryConfig;
import com.networknt.schema.SpecificationVersion;
import com.networknt.schema.resource.IriResourceLoader;
import io.datareplication.model.Body;
import io.datareplication.model.ContentType;
import io.datareplication.model.Url;
import io.datareplication.util.ResourceReader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SnapshotIndexTest {

    public static final String SCHEMA_URL_FOR_SNAPSHOT_INDEX =
        "https://www.datareplication.io/spec/snapshot/index.schema.json";
    private final ObjectMapper mapper = new ObjectMapper();
    private Schema schema;

    @BeforeEach
    void setUp() {
        schema = fetchJsonSchemaFromUrl();
    }

    @Test
    void fromJson_happyPath() throws IOException {
        String json = ResourceReader.readFromInputStream("__files/snapshot/index.json");
        SnapshotIndex snapshotIndex = SnapshotIndex.fromJson(Body.fromUtf8(json));
        assertThat(snapshotIndex.id()).isEqualTo(SnapshotId.of("example"));
        assertThat(snapshotIndex.createdAt()).isEqualTo(Instant.parse("2023-10-07T15:00:00Z"));
        assertThat(snapshotIndex.pages()).contains(
            Url.of("http://localhost:8443/1.content.multipart"),
            Url.of("http://localhost:8443/2.content.multipart"),
            Url.of("http://localhost:8443/3.content.multipart")
        );
    }

    @Test
    void toJson_happyPath() throws IOException {
        SnapshotIndex snapshotIndex = new SnapshotIndex(
            SnapshotId.of("12345678"),
            Instant.parse("2023-09-29T20:52:17.000Z"),
            List.of(
                Url.of("https://localhost:12345/snapshot/12345678/1"),
                Url.of("https://localhost:12345/snapshot/12345678/2"),
                Url.of("https://localhost:12345/snapshot/12345678/3")
            )
        );
        Body snapshotIndexJson = snapshotIndex.toJson();
        assertThat(snapshotIndexJson.toUtf8()).isEqualTo(
            "{\"id\":\"12345678\",\"createdAt\":\"2023-09-29T20:52:17Z\","
                + "\"pages\":["
                + "\"https://localhost:12345/snapshot/12345678/1\","
                + "\"https://localhost:12345/snapshot/12345678/2\","
                + "\"https://localhost:12345/snapshot/12345678/3\""
                + "]}"
        );
        assertThat(snapshotIndexJson.contentType()).isEqualTo(ContentType.of("application/json"));
    }

    @Test
    void schemaValidation_providedJsonValidates() throws IOException {
        JsonNode node = getJsonNodeFromStringContent("{\n"
            + "    \"id\": \"12345678\",\n"
            + "    \"createdAt\": \"2023-09-27T20:52:17.000Z\",\n"
            + "    \"pages\": [\n"
            + "      \"https://example.datareplication.io/12345678/page/1\",\n"
            + "      \"https://example.datareplication.io/12345678/page/2\",\n"
            + "      \"https://example.datareplication.io/12345678/page/3\"\n"
            + "    ]\n"
            + "  }");
        var errors = schema.validate(node);
        assertThat(errors).isEmpty();
    }

    @Test
    void schemaValidation_providedJsonFailsToValidateBecauseItHasMissingKeys() throws IOException {
        JsonNode node = getJsonNodeFromStringContent("{\n"
            + "    \"id1\": \"12345678\",\n"
            + "    \"createdAt2\": \"2023-09-27T20:52:17.000Z\",\n"
            + "    \"pages3\": [\n"
            + "      \"https://example.datareplication.io/12345678/page/1\",\n"
            + "      \"https://example.datareplication.io/12345678/page/2\",\n"
            + "      \"https://example.datareplication.io/12345678/page/3\"\n"
            + "    ]\n"
            + "  }");

        var errors = schema.validate(node);

        assertThat(errors).isNotEmpty();
    }

    @Test
    void schemaValidation_providedJsonFailsToValidateBecauseItHasWrongValues() throws IOException {
        JsonNode node = getJsonNodeFromStringContent("{\n"
            + "    \"id\": \"12345678\",\n"
            + "    \"createdAt\": \"2023-09-99T20:52:17.000Z\",\n"
            + "    \"pages\": \"no array\"\n"
            + "  }");

        var errors = schema.validate(node);

        assertThat(errors).isNotEmpty();
    }

    protected Schema fetchJsonSchemaFromUrl() {
        var registry = SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_2020_12, builder ->
            builder
                .schemaRegistryConfig(
                    SchemaRegistryConfig.builder()
                        .locale(Locale.ROOT)
                        .build()
                )
                .resourceLoaders(loaders -> loaders.add(new IriResourceLoader()))
        );
        return registry.getSchema(SchemaLocation.of(SCHEMA_URL_FOR_SNAPSHOT_INDEX));
    }

    protected JsonNode getJsonNodeFromStringContent(String content) throws IOException {
        return mapper.readTree(content);
    }

    @Test
    void fromJson_throwExceptionBecauseItIsMissingPropertyId() {
        String json = "{\n"
            + "    \"createdAt\": \"2023-09-27T20:52:17.000Z\",\n"
            + "    \"pages\":[]"
            + "}";
        Body body = Body.fromUtf8(json);
        assertThrows(SnapshotIndex.ParsingException.class, () -> SnapshotIndex.fromJson(body));
    }

    @Test
    void fromJson_throwExceptionBecauseItIsMissingPropertyCreatedAt() {
        String json = "{\n"
            + "    \"id\": \"12345678\",\n"
            + "    \"pages\":[]"
            + "}";
        Body body = Body.fromUtf8(json);
        var thrown = assertThrows(SnapshotIndex.ParsingException.class, () -> SnapshotIndex.fromJson(body), "");
        assertThat(thrown.getMessage()).isEqualTo("provided json is missing a property: 'createdAt'");
    }

    @Test
    void fromJson_throwExceptionBecauseItHasInvalidPagesArray() {
        String json = "{\n"
            + "    \"id\": \"12345678\",\n"
            + "    \"createdAt\": \"2023-09-27T20:52:17.000Z\",\n"
            + "    \"pages\":\"not an array\""
            + "}";
        Body body = Body.fromUtf8(json);
        var thrown = assertThrows(SnapshotIndex.ParsingException.class, () -> SnapshotIndex.fromJson(body));
        assertThat(thrown.getMessage())
            .startsWith("java.lang.IllegalStateException: "
                + "Expected BEGIN_ARRAY but was STRING at line 4 column 14 path $.pages");
    }

    @Test
    void fromJson_throwExceptionBecauseItIsMissingPropertyPages() {
        String json = "{\n"
            + "    \"id\": \"12345678\",\n"
            + "    \"createdAt\": \"2023-09-27T20:52:17.000Z\"\n"
            + "}";
        Body body = Body.fromUtf8(json);
        var thrown = assertThrows(SnapshotIndex.ParsingException.class, () -> SnapshotIndex.fromJson(body));
        assertThat(thrown.getMessage()).isEqualTo("provided json is missing a property: 'pages'");

    }

    @Test
    void fromJson_throwExceptionBecauseItIsMalformedJson() {
        String json = "hello index";
        Body body = Body.fromUtf8(json);
        var thrown = assertThrows(SnapshotIndex.ParsingException.class, () -> SnapshotIndex.fromJson(body));
        assertThat(thrown.getMessage())
            .startsWith("java.lang.IllegalStateException: "
                + "Expected BEGIN_OBJECT but was STRING at line 1 column 1 path $");
    }

    private static final Url URL_1 = Url.of("https://example.datareplication.io/1");
    private static final Url URL_2 = Url.of("https://example.datareplication.io/2");
    private static final Url URL_3 = Url.of("https://example.datareplication.io/3");

    @Test
    void shouldMakePageListUnmodifiable() {
        final ArrayList<Url> original = new ArrayList<>();
        original.add(URL_1);
        original.add(URL_2);
        final ArrayList<Url> pages = new ArrayList<>(original);

        final SnapshotIndex index = new SnapshotIndex(SnapshotId.of("1234"), Instant.now(), pages);

        assertThat(index.pages()).containsExactlyElementsOf(original);
        assertThatThrownBy(() -> index.pages().add(URL_3))
            .isInstanceOf(UnsupportedOperationException.class);
        pages.add(URL_3);
        assertThat(index.pages()).containsExactlyElementsOf(original);
    }
}
