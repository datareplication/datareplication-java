package io.datareplication.model.snapshot;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.datareplication.model.Body;
import io.datareplication.model.Timestamp;
import io.datareplication.model.Url;
import lombok.NonNull;
import lombok.Value;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

@Value
public class SnapshotIndex {
    @NonNull SnapshotId id;
    @NonNull Timestamp createdAt;
    @NonNull List<@NonNull Url> pages;

    public SnapshotIndex(@NonNull SnapshotId id, @NonNull Timestamp createdAt, @NonNull List<@NonNull Url> pages) {
        this.id = id;
        this.createdAt = createdAt;
        this.pages = Collections.unmodifiableList(pages);
    }

    public @NonNull Body toJson() {
        Gson gson = new GsonBuilder()
            .registerTypeAdapter(SnapshotIndex.class, new SnapshotIndexSerializer())
            .create();
        return Body.fromUtf8(gson.toJson(this));
    }

    // TODO: error handling
    public static @NonNull SnapshotIndex fromJson(@NonNull Body json) throws IOException {
        Gson gson = new GsonBuilder()
            .registerTypeAdapter(SnapshotIndex.class, new SnapshotIndexDeserializer())
            .create();
        return gson.fromJson(json.toUtf8(), SnapshotIndex.class);
    }

}
