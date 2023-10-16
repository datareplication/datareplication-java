package io.datareplication.model.snapshot;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class SnapshotIndexGsonUtil {

    private static Gson instance;

    private SnapshotIndexGsonUtil() {
    }

    public static Gson getInstance() {
        if (instance == null) {
            instance = new GsonBuilder()
                .registerTypeAdapter(SnapshotIndex.class, new SnapshotIndexSerializer())
                .registerTypeAdapter(SnapshotIndex.class, new SnapshotIndexDeserializer())
                .create();
        }
        return instance;
    }


}
