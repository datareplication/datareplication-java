package io.datareplication.feed.producer;

import io.datareplication.model.Body;
import io.datareplication.model.Entity;
import io.datareplication.model.feed.FeedEntityHeader;
import io.datareplication.model.feed.OperationType;
import lombok.NonNull;

import java.util.Optional;
import java.util.concurrent.CompletionStage;

public interface FeedProducer {
    @NonNull CompletionStage<Void> publish(@NonNull OperationType operationType, @NonNull Body body);

    @NonNull CompletionStage<Void> publish(@NonNull OperationType operationType,
                                           @NonNull Body body,
                                           @NonNull Object userData);

    @NonNull CompletionStage<Void> publish(@NonNull OperationType operationType,
                                           @NonNull Body body,
                                           @NonNull Optional<@NonNull Object> userData);

    @NonNull CompletionStage<Void> publish(@NonNull Entity<@NonNull FeedEntityHeader> entity);

    @NonNull CompletionStage<Void> assignPages();

    @NonNull FeedPageProvider pageProvider();
}
