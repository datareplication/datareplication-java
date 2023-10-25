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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.reactivestreams.FlowAdapters;
import reactor.core.publisher.Flux;

import java.time.Clock;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SnapshotProducerImplTest {
    private static final int MAX_BYTES_PER_PAGE = 5;
    private final SnapshotId id = SnapshotId.of("1234");
    private final PageId pageId1 = PageId.of("page1");
    private final PageId pageId2 = PageId.of("page2");
    private final PageId pageId3 = PageId.of("page3");
    private final PageId pageId4 = PageId.of("page4");
    private final Url page1Url = Url.of("/" + pageId1.value());
    private final Url page2Url = Url.of("/" + pageId2.value());
    private final Url page3Url = Url.of("/" + pageId3.value());
    private final Url page4Url = Url.of("/" + pageId4.value());
    private final Timestamp createdAt = Timestamp.now();

    @Captor
    ArgumentCaptor<Page<SnapshotPageHeader, SnapshotEntityHeader>> pageArgumentCaptor;

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
        SnapshotProducer snapshotProducer = SnapshotProducer
            .builder()
            .pageIdProvider(pageIdProvider)
            .snapshotIdProvider(snapshotIdProvider)
            .maxBytesPerPage(MAX_BYTES_PER_PAGE)
            // TODO: Additional configuration
            .build(snapshotIndexRepository,
                snapshotPageRepository,
                snapshotPageUrlBuilder,
                Clock.fixed(createdAt.value(), ZoneId.systemDefault()));

        CompletionStage<SnapshotIndex> produce =
            snapshotProducer.produce(FlowAdapters.toFlowPublisher(Flux.empty()));

        SnapshotIndex snapshotIndex = produce.toCompletableFuture().get();
        assertThat(snapshotIndex)
            .isEqualTo(new SnapshotIndex(id, createdAt, Collections.emptyList()));
        verifyNoInteractions(snapshotPageRepository);
        verify(snapshotIndexRepository).save(snapshotIndex);
    }

    @Test
    @DisplayName("should produce a snapshot with one entry")
    void shouldProduceSingletonSnapshot(@Mock SnapshotPageRepository snapshotPageRepository,
                                        @Mock SnapshotIndexRepository snapshotIndexRepository,
                                        @Mock SnapshotPageUrlBuilder snapshotPageUrlBuilder,
                                        @Mock PageIdProvider pageIdProvider,
                                        @Mock SnapshotIdProvider snapshotIdProvider)
        throws ExecutionException, InterruptedException {
        Entity<SnapshotEntityHeader> firstEntity = entity("there can be only one");
        when(snapshotIdProvider.newSnapshotId()).thenReturn(id);
        when(pageIdProvider.newPageId()).thenReturn(pageId1);
        when(snapshotIndexRepository.save(any(SnapshotIndex.class)))
            .thenReturn(CompletableFuture.supplyAsync(() -> null));
        when(snapshotPageUrlBuilder.pageUrl(id, pageId1)).thenReturn(page1Url);
        when(snapshotPageRepository.save(eq(id), eq(pageId1), any()))
            .thenReturn(CompletableFuture.supplyAsync(() -> null));
        SnapshotProducer snapshotProducer = SnapshotProducer
            .builder()
            .pageIdProvider(pageIdProvider)
            .snapshotIdProvider(snapshotIdProvider)
            .maxBytesPerPage(MAX_BYTES_PER_PAGE)
            .build(snapshotIndexRepository,
                snapshotPageRepository,
                snapshotPageUrlBuilder,
                Clock.fixed(createdAt.value(), ZoneId.systemDefault()));

        CompletionStage<SnapshotIndex> produce =
            snapshotProducer.produce(FlowAdapters.toFlowPublisher(Flux.just(firstEntity)));

        SnapshotIndex snapshotIndex = produce.toCompletableFuture().get();
        assertThat(snapshotIndex).isEqualTo(new SnapshotIndex(id, createdAt, List.of(page1Url)));
        verify(snapshotPageRepository).save(eq(id), eq(pageId1), pageArgumentCaptor.capture());
        verify(snapshotIndexRepository).save(snapshotIndex);
        var savedPage = pageArgumentCaptor.getValue();
        assertThat(savedPage.entities()).isEqualTo(List.of(firstEntity));
        assertThat(savedPage.header()).isEqualTo(new SnapshotPageHeader(HttpHeaders.EMPTY));
        assertThat(savedPage.boundary()).startsWith("_---_");
    }

    @Test
    @DisplayName("should produce a snapshot with multiple entries (buffered by ContentLength)")
    void shouldProduceSnapshotContentLength(@Mock SnapshotPageRepository snapshotPageRepository,
                               @Mock SnapshotIndexRepository snapshotIndexRepository,
                               @Mock SnapshotPageUrlBuilder snapshotPageUrlBuilder,
                               @Mock PageIdProvider pageIdProvider,
                               @Mock SnapshotIdProvider snapshotIdProvider)
        throws ExecutionException, InterruptedException {
        List<Entity<SnapshotEntityHeader>> entities = Stream
            .of("Hello", "World", "I", "am", "a", "Snapshot").map(this::entity)
            .collect(Collectors.toUnmodifiableList());
        Flux<Entity<SnapshotEntityHeader>> entityFlow = Flux.fromIterable(entities);
        when(snapshotIdProvider.newSnapshotId()).thenReturn(id);
        when(pageIdProvider.newPageId()).thenReturn(pageId1, pageId2, pageId3, pageId4);
        when(snapshotIndexRepository.save(any(SnapshotIndex.class)))
            .thenReturn(CompletableFuture.supplyAsync(() -> null));
        when(snapshotPageUrlBuilder.pageUrl(id, pageId1)).thenReturn(page1Url);
        when(snapshotPageUrlBuilder.pageUrl(id, pageId2)).thenReturn(page2Url);
        when(snapshotPageUrlBuilder.pageUrl(id, pageId3)).thenReturn(page3Url);
        when(snapshotPageUrlBuilder.pageUrl(id, pageId4)).thenReturn(page4Url);
        when(snapshotPageRepository.save(eq(id), any(), any()))
            .thenReturn(CompletableFuture.supplyAsync(() -> null));
        SnapshotProducer snapshotProducer = SnapshotProducer
            .builder()
            .pageIdProvider(pageIdProvider)
            .snapshotIdProvider(snapshotIdProvider)
            .maxBytesPerPage(MAX_BYTES_PER_PAGE)
            .build(snapshotIndexRepository,
                snapshotPageRepository,
                snapshotPageUrlBuilder,
                Clock.fixed(createdAt.value(), ZoneId.systemDefault()));

        CompletionStage<SnapshotIndex> produce =
            snapshotProducer.produce(FlowAdapters.toFlowPublisher(entityFlow));

        SnapshotIndex snapshotIndex = produce.toCompletableFuture().get();
        assertThat(snapshotIndex.pages()).containsExactlyInAnyOrder(page1Url, page2Url, page3Url, page4Url);
        assertThat(snapshotIndex.id()).isEqualTo(id);
        assertThat(snapshotIndex.createdAt()).isEqualTo(createdAt);

        verify(snapshotPageRepository).save(eq(id), eq(pageId1), pageArgumentCaptor.capture());
        assertThat(pageArgumentCaptor.getValue().entities()).isEqualTo(List.of(entity("Hello")));
        verify(snapshotPageRepository).save(eq(id), eq(pageId2), pageArgumentCaptor.capture());
        assertThat(pageArgumentCaptor.getValue().entities()).isEqualTo(List.of(entity("World")));
        verify(snapshotPageRepository).save(eq(id), eq(pageId3), pageArgumentCaptor.capture());
        assertThat(pageArgumentCaptor.getValue().entities())
            .isEqualTo(Stream.of("I", "am", "a").map(this::entity).collect(Collectors.toUnmodifiableList()));
        verify(snapshotPageRepository).save(eq(id), eq(pageId4), pageArgumentCaptor.capture());
        assertThat(pageArgumentCaptor.getValue().entities()).isEqualTo(List.of(entity("Snapshot")));
    }

    @Test
    @DisplayName("should produce a snapshot with multiple entries (buffered by MaxEntries)")
    void shouldProduceSnapshotMaxEntries(@Mock SnapshotPageRepository snapshotPageRepository,
                               @Mock SnapshotIndexRepository snapshotIndexRepository,
                               @Mock SnapshotPageUrlBuilder snapshotPageUrlBuilder,
                               @Mock PageIdProvider pageIdProvider,
                               @Mock SnapshotIdProvider snapshotIdProvider)
        throws ExecutionException, InterruptedException {
        List<Entity<SnapshotEntityHeader>> entities = Stream
            .of("Hello", "World", "I", "am", "a", "Snapshot").map(this::entity)
            .collect(Collectors.toUnmodifiableList());
        Flux<Entity<SnapshotEntityHeader>> entityFlow = Flux.fromIterable(entities);
        when(snapshotIdProvider.newSnapshotId()).thenReturn(id);
        when(pageIdProvider.newPageId()).thenReturn(pageId1, pageId2, pageId3);
        when(snapshotIndexRepository.save(any(SnapshotIndex.class)))
            .thenReturn(CompletableFuture.supplyAsync(() -> null));
        when(snapshotPageUrlBuilder.pageUrl(id, pageId1)).thenReturn(page1Url);
        when(snapshotPageUrlBuilder.pageUrl(id, pageId2)).thenReturn(page2Url);
        when(snapshotPageUrlBuilder.pageUrl(id, pageId3)).thenReturn(page3Url);
        when(snapshotPageRepository.save(eq(id), any(), any()))
            .thenReturn(CompletableFuture.supplyAsync(() -> null));
        SnapshotProducer snapshotProducer = SnapshotProducer
            .builder()
            .pageIdProvider(pageIdProvider)
            .snapshotIdProvider(snapshotIdProvider)
            .maxEntriesPerPage(2L)
            .build(snapshotIndexRepository,
                snapshotPageRepository,
                snapshotPageUrlBuilder,
                Clock.fixed(createdAt.value(), ZoneId.systemDefault()));

        CompletionStage<SnapshotIndex> produce =
            snapshotProducer.produce(FlowAdapters.toFlowPublisher(entityFlow));

        SnapshotIndex snapshotIndex = produce.toCompletableFuture().get();
        assertThat(snapshotIndex.pages()).containsExactlyInAnyOrder(page1Url, page2Url, page3Url);
        assertThat(snapshotIndex.id()).isEqualTo(id);
        assertThat(snapshotIndex.createdAt()).isEqualTo(createdAt);

        verify(snapshotPageRepository).save(eq(id), eq(pageId1), pageArgumentCaptor.capture());
        assertThat(pageArgumentCaptor.getValue().entities())
            .isEqualTo(Stream.of("Hello", "World").map(this::entity).collect(Collectors.toUnmodifiableList()));
        verify(snapshotPageRepository).save(eq(id), eq(pageId2), pageArgumentCaptor.capture());
        assertThat(pageArgumentCaptor.getValue().entities())
            .isEqualTo(Stream.of("I", "am").map(this::entity).collect(Collectors.toUnmodifiableList()));
        verify(snapshotPageRepository).save(eq(id), eq(pageId3), pageArgumentCaptor.capture());
        assertThat(pageArgumentCaptor.getValue().entities())
            .isEqualTo(Stream.of("a", "Snapshot").map(this::entity).collect(Collectors.toUnmodifiableList()));
    }

    private Entity<SnapshotEntityHeader> entity(String body) {
        return new Entity<>(new SnapshotEntityHeader(HttpHeaders.EMPTY), Body.fromUtf8(body));
    }
}
