package io.datareplication.model.snapshot;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.datareplication.model.Timestamp;
import io.datareplication.model.Url;

import java.lang.reflect.Type;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

class SnapshotIndexDeserializer implements JsonDeserializer<SnapshotIndex> {
    @Override
    public SnapshotIndex deserialize(JsonElement json,
                                     Type typeOfT,
                                     JsonDeserializationContext context) {
        JsonObject jsonObject = json.getAsJsonObject();

        checkForCompleteness(jsonObject);

        SnapshotId snapshotId = SnapshotId.of(jsonObject.get("id").getAsString());

        String iso8601Timestamp = jsonObject.get("createdAt").getAsString();
        Timestamp timestamp = Timestamp.of(Instant.parse(iso8601Timestamp));

        JsonArray pagesArray = jsonObject.getAsJsonArray("pages");

        List<Url> pages = new ArrayList<>();
        pagesArray.asList().forEach(
            page -> pages.add(Url.of(page.getAsString()))
        );

        return new SnapshotIndex(
            snapshotId,
            timestamp,
            pages
        );
    }

    private void checkForCompleteness(JsonObject jsonObject) {
        if (jsonObject.get("id") == null) {
            throw new IllegalArgumentException("provided json is missing a property: 'id'");
        }
        if (jsonObject.get("createdAt") == null) {
            throw new IllegalArgumentException("provided json is missing a property: 'createdAt'");
        }
        if (jsonObject.get("pages") == null) {
            throw new IllegalArgumentException("provided json is missing a property: 'pages'");
        }
    }
}
