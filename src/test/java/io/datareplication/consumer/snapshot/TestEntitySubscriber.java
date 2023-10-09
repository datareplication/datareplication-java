package io.datareplication.consumer.snapshot;

import io.datareplication.model.Entity;
import io.datareplication.model.snapshot.SnapshotEntityHeader;
import lombok.Getter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Flow;

public class TestEntitySubscriber implements Flow.Subscriber<Entity<SnapshotEntityHeader>> {
    private Flow.Subscription subscription;
    @Getter
    private final List<String> consumedEntities = new ArrayList<>();
    @Getter
    private boolean completed = false;

    @Override
    public void onSubscribe(final Flow.Subscription subscription) {
        this.subscription = subscription;
        subscription.request(1);
    }

    @Override
    public void onNext(final Entity<SnapshotEntityHeader> item) {
        try {
            consumedEntities.add(item.body().toUtf8());
        } catch (IOException e) {
            onError(e);
        }
        subscription.request(1);
    }

    @Override
    public void onError(final Throwable throwable) {
        throw new RuntimeException(throwable);
    }

    @Override
    public void onComplete() {
        completed = true;
    }
}
