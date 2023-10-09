package io.datareplication.consumer.snapshot;

import io.datareplication.model.Entity;
import io.datareplication.model.snapshot.SnapshotEntityHeader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Flow;

public class SnapshotEntitySubscriber implements Flow.Subscriber<Entity<SnapshotEntityHeader>> {
    private Flow.Subscription subscription;
    private final List<String> consumedEntities = new ArrayList<>();

    private boolean completed = false;

    @Override
    public void onSubscribe(final Flow.Subscription subscription) {
        this.subscription = subscription;
        subscription.request(1);
    }

    @Override
    public void onNext(final Entity<SnapshotEntityHeader> item) {
        consumedEntities.add(item.body().toUtf8());
        subscription.request(1);
    }

    @Override
    public void onError(final Throwable throwable) {
        throw new SnapshotEntitySubscriberException(throwable);
    }

    @Override
    public void onComplete() {
        completed = true;
    }

    public boolean hasCompleted() {
        return completed;
    }

    public List<String> getConsumedEntities() {
        return Collections.unmodifiableList(consumedEntities);
    }

    public static class SnapshotEntitySubscriberException extends RuntimeException {
        public SnapshotEntitySubscriberException(final Throwable throwable) {
            super(throwable);
        }
    }
}
