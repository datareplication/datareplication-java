package io.datareplication.model.snapshot;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.datareplication.model.Body;
import io.datareplication.model.Timestamp;
import io.datareplication.model.Url;
import lombok.NonNull;
import lombok.Value;

import java.io.IOException;
import java.util.List;

/**
 * The index of a snapshot. In addition to some metadata, this primarily contains of a list of URLs to the pages that
 * make up this snapshot.
 */
@Value
public class SnapshotIndex {
    /**
     * The ID of the snapshot.
     */
    @NonNull SnapshotId id;
    /**
     * The timestamp of the dataset contained in this snapshot. This is usually the time when snapshot creation started.
     */
    @NonNull Timestamp createdAt;
    /**
     * The URLs of the pages that make up this snapshot.
     */
    @NonNull List<@NonNull Url> pages;

    public SnapshotIndex(@NonNull SnapshotId id, @NonNull Timestamp createdAt, @NonNull List<@NonNull Url> pages) {
        this.id = id;
        this.createdAt = createdAt;
        this.pages = List.copyOf(pages);
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
