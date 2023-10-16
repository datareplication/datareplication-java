package io.datareplication.model.snapshot;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;

class SnapshotIndexSerializer implements JsonSerializer<SnapshotIndex> {

    public JsonElement serialize(SnapshotIndex src,
                                 Type type,
                                 JsonSerializationContext jsonSerializationContext) {
        JsonObject jsonSnapshotIndex = new JsonObject();

        JsonArray pagesArray = new JsonArray();
        src.pages().forEach(
            page -> pagesArray.add(page.value())
        );

        jsonSnapshotIndex.addProperty("id", src.id().value());
        jsonSnapshotIndex.addProperty("createdAt", src.createdAt().value().toString());
        jsonSnapshotIndex.add("pages", pagesArray);

        return jsonSnapshotIndex;
    }
}
