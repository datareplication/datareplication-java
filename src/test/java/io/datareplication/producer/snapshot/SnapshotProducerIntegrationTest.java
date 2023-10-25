package io.datareplication.producer.snapshot;

import io.datareplication.model.Body;
import io.datareplication.model.Entity;
import io.datareplication.model.HttpHeader;
import io.datareplication.model.HttpHeaders;
import io.datareplication.model.Page;
import io.datareplication.model.PageId;
import io.datareplication.model.Timestamp;
import io.datareplication.model.Url;
import io.datareplication.model.snapshot.SnapshotEntityHeader;
import io.datareplication.model.snapshot.SnapshotId;
import io.datareplication.model.snapshot.SnapshotIndex;
import io.datareplication.model.snapshot.SnapshotPageHeader;
import lombok.NonNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.reactivestreams.FlowAdapters;
import reactor.core.publisher.Flux;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SnapshotProducerIntegrationTest {
    Timestamp now = Timestamp.of(Instant.now());

    @Test
    @DisplayName("Should produce snapshot")
    void shouldProduceSnapshot(@Mock SnapshotPageRepository snapshotPageRepository,
                               @Mock SnapshotIndexRepository snapshotIndexRepository,
                               @Mock SnapshotPageUrlBuilder snapshotPageUrlBuilder,
                               @Mock PageIdProvider pageIdProvider,
                               @Mock SnapshotIdProvider snapshotIdProvider)
        throws ExecutionException, InterruptedException {
        Flux<Entity<SnapshotEntityHeader>> entityFlow = Flux
            .just("Hello", "World", "I", "am", "a", "snapshot")
            .map(this::toSnapshotEntity);

        SnapshotId snapshotId = SnapshotId.of("snapshotIdForTest");
        PageId pageId1 = PageId.of("1");
        PageId pageId2 = PageId.of("2");
        PageId pageId3 = PageId.of("3");
        List<Url> pages = List.of(
            url(snapshotId, pageId1),
            url(snapshotId, pageId2),
            url(snapshotId, pageId3)
        );

        when(pageIdProvider.newPageId()).thenReturn(pageId1).thenReturn(pageId2).thenReturn(pageId3);
        when(snapshotIdProvider.newSnapshotId()).thenReturn(snapshotId);
        when(snapshotPageUrlBuilder.pageUrl(any(SnapshotId.class), any(PageId.class))).thenAnswer(input ->
            url(input.getArgument(0), input.getArgument(1)));

        SnapshotIndex expectedSnapshotIndex = new SnapshotIndex(snapshotId, now, pages);

        SnapshotProducer snapshotProducer = SnapshotProducer
            .builder()
            .pageIdProvider(pageIdProvider)
            .snapshotIdProvider(snapshotIdProvider)
            .maxBytesPerPage(2)
            // TODO: Additional configuration
            .build(snapshotIndexRepository, snapshotPageRepository, snapshotPageUrlBuilder);


        SnapshotIndex result = snapshotProducer
            .produce(FlowAdapters.toFlowPublisher(entityFlow))
            .toCompletableFuture()
            .get();

        assertThat(result).isEqualTo(expectedSnapshotIndex);
        verify(snapshotIndexRepository).save(expectedSnapshotIndex);
        // TODO: Add SnapshotPageHeader
        verify(snapshotPageRepository).save(
            snapshotId,
            pageId1,
            new Page<>(
                new SnapshotPageHeader(HttpHeaders.EMPTY),
                List.of(
                    toSnapshotEntity("Hello"),
                    toSnapshotEntity("World"))
            )
        );
        verify(snapshotPageRepository).save(
            snapshotId,
            pageId2,
            new Page<>(new SnapshotPageHeader(HttpHeaders.EMPTY),
                List.of(
                    toSnapshotEntity("I"),
                    toSnapshotEntity("am"))
            )
        );
        verify(snapshotPageRepository).save(
            snapshotId,
            pageId3,
            new Page<>(new SnapshotPageHeader(HttpHeaders.EMPTY),
                List.of(
                    toSnapshotEntity("a"),
                    toSnapshotEntity("snapshot"))
            )
        );
    }

    private @NonNull Url url(final SnapshotId snapshotId, final PageId pageId1) {
        return Url.of("https://localhost:8080/" + snapshotId.value() + "/" + pageId1.value());
    }

    private @NonNull Entity<@NonNull SnapshotEntityHeader> toSnapshotEntity(String content) {
        SnapshotEntityHeader snapshotEntityHeader = new SnapshotEntityHeader(
            HttpHeaders.of(
                // TODO: Must not be optional
                HttpHeader.lastModified(now)
            )
        );
        return new Entity<>(snapshotEntityHeader, Body.fromUtf8(content));
    }
}
