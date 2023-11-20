package io.datareplication.model.snapshot;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;
import com.google.gson.JsonSyntaxException;
import io.datareplication.model.Timestamp;
import io.datareplication.model.Url;
import lombok.experimental.UtilityClass;

import java.time.Instant;
import java.time.format.DateTimeParseException;

@UtilityClass
final class SnapshotIndexJsonCodec {
    private static final Gson GSON = new GsonBuilder()
        .registerTypeAdapter(SnapshotId.class, (JsonSerializer<SnapshotId>)
            (snapshotId, type, jsonSerializationContext) -> new JsonPrimitive(snapshotId.value()))
        .registerTypeAdapter(Timestamp.class, (JsonSerializer<Timestamp>)
            (timestamp, type, jsonDeserializationContext) -> new JsonPrimitive(timestamp.value().toString()))
        .registerTypeAdapter(Url.class, (JsonSerializer<Url>)
            (url, type, jsonDeserializationContext) -> new JsonPrimitive(url.value()))
        .registerTypeAdapter(SnapshotId.class, (JsonDeserializer<SnapshotId>)
            (jsonElement, type, jsonSerializationContext) -> SnapshotId.of(jsonElement.getAsString()))
        .registerTypeAdapter(Timestamp.class, (JsonDeserializer<Timestamp>)
            (jsonElement, type, jsonDeserializationContext) -> Timestamp.of(Instant.parse(jsonElement.getAsString())))
        .registerTypeAdapter(Url.class, (JsonDeserializer<Url>)
            (jsonElement, type, jsonDeserializationContext) -> Url.of(jsonElement.getAsString()))
        .create();

    public static SnapshotIndex fromJson(String json) {
        try {
            SnapshotIndex snapshotIndex = GSON.fromJson(json, SnapshotIndex.class);

            // The JSON deserializer leaves fields as null if they're missing from the JSON, despite the @NonNull
            // annotation. IntelliJ doesn't know this, so we just disable the check here.
            //noinspection ConstantValue
            if (snapshotIndex.id() == null) {
                throw new IllegalArgumentException("provided json is missing a property: 'id'");
            }
            //noinspection ConstantValue
            if (snapshotIndex.createdAt() == null) {
                throw new IllegalArgumentException("provided json is missing a property: 'createdAt'");
            }
            //noinspection ConstantValue
            if (snapshotIndex.pages() == null) {
                throw new IllegalArgumentException("provided json is missing a property: 'pages'");
            }
            return snapshotIndex;
        } catch (IllegalArgumentException | DateTimeParseException | JsonSyntaxException ex) {
            throw new SnapshotIndex.ParsingException(ex);
        }
    }

    public static String toJson(SnapshotIndex index) {
        return GSON.toJson(index);
    }
}
