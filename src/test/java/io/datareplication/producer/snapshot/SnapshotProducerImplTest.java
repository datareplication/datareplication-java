package io.datareplication.producer.snapshot;

import io.datareplication.model.Body;
import io.datareplication.model.Entity;
import io.datareplication.model.HttpHeaders;
import io.datareplication.model.Page;
import io.datareplication.model.PageId;
import io.datareplication.model.Timestamp;
import io.datareplication.model.Url;
import io.datareplication.model.snapshot.SnapshotEntityHeader;
import io.datareplication.model.snapshot.SnapshotId;
import io.datareplication.model.snapshot.SnapshotIndex;
import io.datareplication.model.snapshot.SnapshotPageHeader;
import io.reactivex.rxjava3.core.Flowable;
import lombok.NonNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.reactivestreams.FlowAdapters;

import java.time.Clock;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SnapshotProducerImplTest {
    private static final int MAX_BYTES_PER_PAGE = 1000;
    private final SnapshotId id = SnapshotId.of("1234");
    private final PageId pageId1 = PageId.of("page1");
    private final Url page1Url = Url.of("/" + pageId1.value());
    private final Timestamp createdAt = Timestamp.now();

    @Test
    @DisplayName("should produce a snapshot without entries")
    void shouldProduceEmptySnapshot(@Mock SnapshotPageRepository snapshotPageRepository,
                                    @Mock SnapshotIndexRepository snapshotIndexRepository,
                                    @Mock SnapshotPageUrlBuilder snapshotPageUrlBuilder,
                                    @Mock PageIdProvider pageIdProvider,
                                    @Mock SnapshotIdProvider snapshotIdProvider)
        throws ExecutionException, InterruptedException {
        when(snapshotIdProvider.newSnapshotId()).thenReturn(id);
        when(snapshotIndexRepository.save(any(SnapshotIndex.class)))
            .thenReturn(CompletableFuture.supplyAsync(() -> null));
        SnapshotProducer snapshotProducer = producer(
            snapshotPageRepository,
            snapshotIndexRepository,
            snapshotPageUrlBuilder,
            pageIdProvider,
            snapshotIdProvider
        );

        CompletionStage<SnapshotIndex> produce =
            snapshotProducer.produce(FlowAdapters.toFlowPublisher(Flowable.empty()));

        SnapshotIndex snapshotIndex = produce.toCompletableFuture().get();
        assertThat(snapshotIndex)
            .isEqualTo(new SnapshotIndex(id, createdAt, Collections.emptyList()));
        verifyNoInteractions(snapshotPageRepository);
        verify(snapshotIndexRepository).save(snapshotIndex);
    }

    @Test
    @DisplayName("should produce a snapshot with one entry")
    void shouldSingletonSnapshot(@Mock SnapshotPageRepository snapshotPageRepository,
                                 @Mock SnapshotIndexRepository snapshotIndexRepository,
                                 @Mock SnapshotPageUrlBuilder snapshotPageUrlBuilder,
                                 @Mock PageIdProvider pageIdProvider,
                                 @Mock SnapshotIdProvider snapshotIdProvider)
        throws ExecutionException, InterruptedException {
        Entity<SnapshotEntityHeader> firstEntity = entity("there can be only one");
        Page<SnapshotPageHeader, SnapshotEntityHeader> page1 = new Page<>(
            new SnapshotPageHeader(HttpHeaders.EMPTY),
            List.of(firstEntity)
        );
        when(snapshotIdProvider.newSnapshotId()).thenReturn(id);
        when(pageIdProvider.newPageId()).thenReturn(pageId1);
        when(snapshotIndexRepository.save(any(SnapshotIndex.class)))
            .thenReturn(CompletableFuture.supplyAsync(() -> null));
        when(snapshotPageUrlBuilder.pageUrl(id, pageId1)).thenReturn(page1Url);
        when(snapshotPageRepository.save(id, pageId1, page1))
            .thenReturn(CompletableFuture.supplyAsync(() -> null));
        SnapshotProducer snapshotProducer = producer(
            snapshotPageRepository,
            snapshotIndexRepository,
            snapshotPageUrlBuilder,
            pageIdProvider,
            snapshotIdProvider
        );

        CompletionStage<SnapshotIndex> produce =
            snapshotProducer.produce(FlowAdapters.toFlowPublisher(Flowable.just(firstEntity)));

        SnapshotIndex snapshotIndex = produce.toCompletableFuture().get();
        assertThat(snapshotIndex)
            .isEqualTo(new SnapshotIndex(id, createdAt, List.of(page1Url)));
        verify(snapshotPageRepository).save(
            id,
            pageId1,
            new Page<@NonNull SnapshotPageHeader, @NonNull SnapshotEntityHeader>(
                new SnapshotPageHeader(HttpHeaders.EMPTY),
                List.of(firstEntity)
            )
        );
        verify(snapshotIndexRepository).save(snapshotIndex);
    }

    private SnapshotProducer producer(SnapshotPageRepository snapshotPageRepository,
                                      SnapshotIndexRepository snapshotIndexRepository,
                                      SnapshotPageUrlBuilder snapshotPageUrlBuilder,
                                      PageIdProvider pageIdProvider,
                                      SnapshotIdProvider snapshotIdProvider) {
        return SnapshotProducer
            .builder()
            .pageIdProvider(pageIdProvider)
            .snapshotIdProvider(snapshotIdProvider)
            .maxBytesPerPage(MAX_BYTES_PER_PAGE)
            // TODO: Additional configuration
            .build(snapshotIndexRepository,
                snapshotPageRepository,
                snapshotPageUrlBuilder,
                Clock.fixed(createdAt.value(), ZoneId.systemDefault()));
    }

    private Entity<SnapshotEntityHeader> entity(String body) {
        return new Entity<>(new SnapshotEntityHeader(HttpHeaders.EMPTY), Body.fromUtf8(body));
    }
}
