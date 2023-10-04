package io.datareplication.model.snapshot;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SchemaValidatorsConfig;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

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
    void providedJsonValidatesToJsonSchema() throws IOException {
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
    void providedJsonFailsToValidateBecauseItHasMissingKeys() throws IOException {
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
    void providedJsonFailsToValidateBecauseItHasWrongValues() throws IOException {
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
}
