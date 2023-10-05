package io.datareplication.model.snapshot;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SchemaValidatorsConfig;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import io.datareplication.model.Body;
import io.datareplication.model.Timestamp;
import io.datareplication.model.Url;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SnapshotIndexTest {

    public static final String SCHEMA_URL_FOR_SNAPSHOT_INDEX =
        "https://www.datareplication.io/spec/snapshot/index.schema.json";
    private final ObjectMapper mapper = new ObjectMapper();
    private JsonSchema schema;

    @BeforeEach
    void setUp() throws URISyntaxException {
        schema = fetchJsonSchemaFromUrl();
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
        Set<ValidationMessage> errors = schema.validate(node);
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
        Set<ValidationMessage> errors = schema.validate(node);
        assertThat(errors).hasSize(3);
        assertThat(errors.stream().map(ValidationMessage::toString)).contains("$.id: is missing but it is required");
        assertThat(errors.stream().map(ValidationMessage::toString))
            .contains("$.createdAt: is missing but it is required");
        assertThat(errors.stream().map(ValidationMessage::toString)).contains("$.pages: is missing but it is required");
    }

    @Test
    void schemaValidation_providedJsonFailsToValidateBecauseItHasWrongValues() throws IOException {
        JsonNode node = getJsonNodeFromStringContent("{\n"
                                                     + "    \"id\": \"12345678\",\n"
                                                     + "    \"createdAt\": \"2023-09-99T20:52:17.000Z\",\n"
                                                     + "    \"pages\": \"no array\"\n"
                                                     + "  }");
        Set<ValidationMessage> errors = schema.validate(node);
        assertThat(errors).hasSize(2);
        assertThat(errors.stream().map(ValidationMessage::toString))
            .contains("$.createdAt: 2023-09-99T20:52:17.000Z is an invalid date-time");
        assertThat(errors.stream().map(ValidationMessage::toString))
            .contains("$.pages: string found, array expected");
    }

    protected JsonSchema fetchJsonSchemaFromUrl() throws URISyntaxException {
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
        SchemaValidatorsConfig config = getSchemaValidatorsConfigWithoutLocalization();
        return factory.getSchema(new URI(SCHEMA_URL_FOR_SNAPSHOT_INDEX), config);
    }

    private SchemaValidatorsConfig getSchemaValidatorsConfigWithoutLocalization() {
        SchemaValidatorsConfig config = new SchemaValidatorsConfig();
        config.setLocale(Locale.ROOT);
        return config;
    }

    protected JsonNode getJsonNodeFromStringContent(String content) throws IOException {
        return mapper.readTree(content);
    }

    @Test
    void fromJson_happyPath() throws IOException {
        String json = "{\n"
                      + "    \"id\": \"12345678\",\n"
                      + "    \"createdAt\": \"2023-09-29T20:52:17.000Z\",\n"
                      + "    \"pages\":["
                      + "    \"https://localhost:12345/snapshot/12345678/1\","
                      + "    \"https://localhost:12345/snapshot/12345678/2\","
                      + "    \"https://localhost:12345/snapshot/12345678/3\""
                      + "    ]"
                      + "}";
        SnapshotIndex snapshotIndex = SnapshotIndex.fromJson(Body.fromUtf8(json));
        assertThat(snapshotIndex.id()).isEqualTo(SnapshotId.of("12345678"));
        assertThat(snapshotIndex.createdAt()).isEqualTo(Timestamp.of(Instant.parse("2023-09-29T20:52:17.000Z")));
        assertThat(snapshotIndex.pages()).contains(
            Url.of("https://localhost:12345/snapshot/12345678/1"),
            Url.of("https://localhost:12345/snapshot/12345678/2"),
            Url.of("https://localhost:12345/snapshot/12345678/3")
        );
    }
    @Test
    void fromJson_throwsExceptionBecauseOfMalformattedCreatedAtTime() {
        String json = "{\n"
                      + "    \"id\": \"12345678\",\n"
                      + "    \"createdAt\": \"2023-09-99T20:52:17.000Z\",\n"
                      + "    \"pages\":[]"
                      + "}";
        Body body = Body.fromUtf8(json);
        assertThrows(DateTimeParseException.class, () -> SnapshotIndex.fromJson(body));
    }

    @Test
    void fromJson_throwExceptionBecauseItIsMissingPropertyId() {
        String json = "{\n"
                      + "    \"createdAt\": \"2023-09-99T20:52:17.000Z\",\n"
                      + "    \"pages\":[]"
                      + "}";
        Body body = Body.fromUtf8(json);
        assertThrows(IllegalArgumentException.class, () -> SnapshotIndex.fromJson(body));
    }

    @Test
    void fromJson_throwExceptionBecauseItIsMissingPropertyCreatedAt() {
        String json = "{\n"
                      + "    \"id\": \"12345678\",\n"
                      + "    \"pages\":[]"
                      + "}";
        Body body = Body.fromUtf8(json);
        assertThrows(IllegalArgumentException.class, () -> SnapshotIndex.fromJson(body));
    }

    @Test
    void fromJson_throwExceptionBecauseItIsMissingPropertyPages() {
        String json = "{\n"
                      + "    \"id\": \"12345678\",\n"
                      + "    \"createdAt\": \"2023-09-99T20:52:17.000Z\"\n"
                      + "}";
        Body body = Body.fromUtf8(json);
        assertThrows(IllegalArgumentException.class, () -> SnapshotIndex.fromJson(body));
    }

    @Test
    void toJson_happyPath() throws IOException {
        SnapshotIndex snapshotIndex = new SnapshotIndex(
            SnapshotId.of("12345678"),
            Timestamp.of(Instant.parse("2023-09-29T20:52:17.000Z")),
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
    }
}
