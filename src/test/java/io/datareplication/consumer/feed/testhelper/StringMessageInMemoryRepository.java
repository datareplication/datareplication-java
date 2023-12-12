package io.datareplication.consumer.feed.testhelper;

import io.datareplication.model.feed.OperationType;
import lombok.extern.slf4j.Slf4j;

import java.util.Collection;
import java.util.Collections;
import java.util.SortedMap;
import java.util.TreeMap;

@Slf4j
public class StringMessageInMemoryRepository {
    private final SortedMap<String, String> repository = new TreeMap<>();

    public void save(String message, OperationType operationType) {
        switch (operationType) {
            case PUT:
            case PATCH:
                repository.put(message, message);
                break;
            case DELETE:
                repository.remove(message);
                break;
            default:
                log.error("Unsupported opertion type used: {}", operationType);
                break;
        }
    }

    public Collection<String> getSortedMessages() {
        return Collections.unmodifiableCollection(repository.values());
    }
}
