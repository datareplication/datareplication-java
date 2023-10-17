package io.datareplication.consumer;

import io.datareplication.model.ContentType;
import io.datareplication.model.Entity;
import io.datareplication.model.Page;
import io.datareplication.model.ToHttpHeaders;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.Value;

import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;

public interface StreamingPage<
    PageHeader extends ToHttpHeaders,
    EntityHeader extends ToHttpHeaders
    > extends Flow.Publisher<StreamingPage.Chunk<EntityHeader>> {
    class Chunk<EntityHeader> {
        private Chunk() {
        }

        @Value
        @AllArgsConstructor(access = AccessLevel.PRIVATE)
        @EqualsAndHashCode(callSuper = false)
        public static class Header<EntityHeader> extends Chunk<EntityHeader> {
            @NonNull EntityHeader header;
            long contentLength;
            @NonNull ContentType contentType;
        }

        @Value
        @AllArgsConstructor(access = AccessLevel.PRIVATE)
        @EqualsAndHashCode(callSuper = false)
        public static class BodyChunk<EntityHeader> extends Chunk<EntityHeader> {
            @NonNull ByteBuffer data;
        }

        @Value
        @AllArgsConstructor(access = AccessLevel.PRIVATE)
        @EqualsAndHashCode(callSuper = false)
        public static class BodyEnd<EntityHeader> extends Chunk<EntityHeader> {
            private static final BodyEnd<?> INSTANCE = new BodyEnd<>();
        }

        public static @NonNull <EntityHeader> Header<EntityHeader> header(@NonNull EntityHeader header,
                                                                          long contentLength,
                                                                          @NonNull ContentType contentType) {
            return new Header<>(header, contentLength, contentType);
        }

        public static @NonNull <EntityHeader> BodyChunk<EntityHeader> bodyChunk(@NonNull ByteBuffer data) {
            return new BodyChunk<>(data);
        }

        public static @NonNull <EntityHeader> BodyEnd<EntityHeader> bodyEnd() {
            //noinspection unchecked
            return (BodyEnd<EntityHeader>) BodyEnd.INSTANCE;
        }
    }

    @NonNull PageHeader header();

    @NonNull Flow.Publisher<@NonNull Entity<EntityHeader>> toCompleteEntities();

    @NonNull CompletionStage<@NonNull Page<PageHeader, EntityHeader>> toCompletePage();
}
