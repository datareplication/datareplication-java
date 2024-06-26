package io.datareplication.producer.snapshot;

import io.datareplication.internal.multipart.MultipartUtils;
import io.datareplication.model.Body;
import io.datareplication.model.ContentType;
import io.datareplication.model.Entity;
import io.datareplication.model.HttpHeader;
import io.datareplication.model.HttpHeaders;
import io.datareplication.model.Page;
import io.datareplication.model.PageId;
import io.datareplication.model.Url;
import io.datareplication.model.snapshot.SnapshotEntityHeader;
import io.datareplication.model.snapshot.SnapshotId;
import io.datareplication.model.snapshot.SnapshotIndex;
import io.datareplication.model.snapshot.SnapshotPageHeader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.reactivestreams.FlowAdapters;
import reactor.core.publisher.Flux;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SnapshotProducerImplTest {
    private static final long MAX_BYTES_PER_PAGE = 5;
    private final SnapshotId id = SnapshotId.of("1234");
    private final PageId pageId1 = PageId.of("page1");
    private final PageId pageId2 = PageId.of("page2");
    private final PageId pageId3 = PageId.of("page3");
    private final PageId pageId4 = PageId.of("page4");
    private final Url page1Url = Url.of("/" + pageId1.value());
    private final Url page2Url = Url.of("/" + pageId2.value());
    private final Url page3Url = Url.of("/" + pageId3.value());
    private final Url page4Url = Url.of("/" + pageId4.value());
    private final Instant createdAt = Instant.now();
    private final List<Entity<SnapshotEntityHeader>> entities =
        entities("Hello", "World", "I", "am", "a", "Snapshot");

    @Mock
    private SnapshotPageRepository snapshotPageRepository;
    @Mock
    private SnapshotIndexRepository snapshotIndexRepository;
    @Mock
    private SnapshotPageUrlBuilder snapshotPageUrlBuilder;
    @Mock
    private RandomPageIdProvider pageIdProvider;
    @Mock
    private RandomSnapshotIdProvider snapshotIdProvider;

    private SnapshotProducer newSnapshotProducer(Long maxBytesPerPage, Long maxEntitiesPerPage) {
        return new SnapshotProducerImpl(
            snapshotPageUrlBuilder,
            snapshotIndexRepository,
            snapshotPageRepository,
            pageIdProvider,
            snapshotIdProvider,
            maxBytesPerPage,
            maxEntitiesPerPage,
            Clock.fixed(createdAt, ZoneId.systemDefault())
        );
    }

    private SnapshotProducer newSnapshotProducer(@SuppressWarnings("SameParameterValue") Long maxBytesPerPage) {
        return newSnapshotProducer(maxBytesPerPage, Long.MAX_VALUE);
    }

    @BeforeEach
    void setUp() {
        when(snapshotIdProvider.newSnapshotId()).thenReturn(id);
        when(snapshotIndexRepository.save(any(SnapshotIndex.class)))
            .thenReturn(CompletableFuture.supplyAsync(() -> null));
    }

    @Test
    @DisplayName("should produce a snapshot without entities")
    void shouldProduceEmptySnapshot() throws ExecutionException, InterruptedException {
        SnapshotProducer snapshotProducer = newSnapshotProducer(MAX_BYTES_PER_PAGE);

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
    void shouldProduceSingletonSnapshot()
        throws ExecutionException, InterruptedException {
        List<Entity<SnapshotEntityHeader>> entities = entities("there can be only one");
        when(pageIdProvider.newPageId()).thenReturn(pageId1);
        when(snapshotPageUrlBuilder.pageUrl(id, pageId1)).thenReturn(page1Url);
        when(snapshotPageRepository.save(eq(id), eq(pageId1), any()))
            .thenReturn(CompletableFuture.supplyAsync(() -> null));
        SnapshotProducer snapshotProducer = newSnapshotProducer(MAX_BYTES_PER_PAGE);

        CompletionStage<SnapshotIndex> produce =
            snapshotProducer.produce(FlowAdapters.toFlowPublisher(Flux.fromIterable(entities)));

        SnapshotIndex snapshotIndex = produce.toCompletableFuture().get();
        assertThat(snapshotIndex).isEqualTo(new SnapshotIndex(id, createdAt, List.of(page1Url)));
        verify(snapshotPageRepository).save(id, pageId1,
            new Page<>(new SnapshotPageHeader(HttpHeaders.EMPTY), MultipartUtils.defaultBoundary(pageId1), entities));
        verify(snapshotIndexRepository).save(snapshotIndex);
    }

    @Test
    @DisplayName("should produce a snapshot with multiple entities (buffered by ContentLength)")
    void shouldProduceSnapshotContentLength()
        throws ExecutionException, InterruptedException {
        Flux<Entity<SnapshotEntityHeader>> entityFlow = Flux.fromIterable(entities);
        when(pageIdProvider.newPageId()).thenReturn(pageId1, pageId2, pageId3, pageId4);
        when(snapshotPageUrlBuilder.pageUrl(id, pageId1)).thenReturn(page1Url);
        when(snapshotPageUrlBuilder.pageUrl(id, pageId2)).thenReturn(page2Url);
        when(snapshotPageUrlBuilder.pageUrl(id, pageId3)).thenReturn(page3Url);
        when(snapshotPageUrlBuilder.pageUrl(id, pageId4)).thenReturn(page4Url);
        when(snapshotPageRepository.save(eq(id), any(), any()))
            .thenReturn(CompletableFuture.supplyAsync(() -> null));
        SnapshotProducer snapshotProducer = newSnapshotProducer(MAX_BYTES_PER_PAGE);

        CompletionStage<SnapshotIndex> produce =
            snapshotProducer.produce(FlowAdapters.toFlowPublisher(entityFlow));

        SnapshotIndex snapshotIndex = produce.toCompletableFuture().get();
        assertThat(snapshotIndex.pages()).containsExactly(page1Url, page2Url, page3Url, page4Url);
        assertThat(snapshotIndex.id()).isEqualTo(id);
        assertThat(snapshotIndex.createdAt()).isEqualTo(createdAt);

        verify(snapshotPageRepository).save(id, pageId1, new Page<>(
            new SnapshotPageHeader(HttpHeaders.EMPTY),
            MultipartUtils.defaultBoundary(pageId1),
            entities("Hello")
        ));
        verify(snapshotPageRepository).save(id, pageId2, new Page<>(
            new SnapshotPageHeader(HttpHeaders.EMPTY),
            MultipartUtils.defaultBoundary(pageId2),
            entities("World")
        ));
        verify(snapshotPageRepository).save(id, pageId3, new Page<>(
            new SnapshotPageHeader(HttpHeaders.EMPTY),
            MultipartUtils.defaultBoundary(pageId3),
            entities("I", "am", "a")
        ));
        verify(snapshotPageRepository).save(id, pageId4, new Page<>(
            new SnapshotPageHeader(HttpHeaders.EMPTY),
            MultipartUtils.defaultBoundary(pageId4),
            entities("Snapshot")
        ));
    }

    @Test
    @DisplayName("should produce a snapshot with multiple entities (buffered by MaxEntities)")
    void shouldProduceSnapshotMaxentities()
        throws ExecutionException, InterruptedException {
        Flux<Entity<SnapshotEntityHeader>> entityFlow = Flux.fromIterable(entities);
        when(pageIdProvider.newPageId()).thenReturn(pageId1, pageId2, pageId3);
        when(snapshotPageUrlBuilder.pageUrl(id, pageId1)).thenReturn(page1Url);
        when(snapshotPageUrlBuilder.pageUrl(id, pageId2)).thenReturn(page2Url);
        when(snapshotPageUrlBuilder.pageUrl(id, pageId3)).thenReturn(page3Url);
        when(snapshotPageRepository.save(eq(id), any(), any()))
            .thenReturn(CompletableFuture.supplyAsync(() -> null));
        SnapshotProducer snapshotProducer = newSnapshotProducer(Long.MAX_VALUE, 2L);

        CompletionStage<SnapshotIndex> produce =
            snapshotProducer.produce(FlowAdapters.toFlowPublisher(entityFlow));

        SnapshotIndex snapshotIndex = produce.toCompletableFuture().get();
        assertThat(snapshotIndex.pages()).containsExactly(page1Url, page2Url, page3Url);
        assertThat(snapshotIndex.id()).isEqualTo(id);
        assertThat(snapshotIndex.createdAt()).isEqualTo(createdAt);

        verify(snapshotPageRepository).save(id, pageId1, new Page<>(
            new SnapshotPageHeader(HttpHeaders.EMPTY),
            MultipartUtils.defaultBoundary(pageId1),
            entities("Hello", "World")
        ));
        verify(snapshotPageRepository).save(id, pageId2, new Page<>(
            new SnapshotPageHeader(HttpHeaders.EMPTY),
            MultipartUtils.defaultBoundary(pageId2),
            entities("I", "am")
        ));
        verify(snapshotPageRepository).save(id, pageId3, new Page<>(
            new SnapshotPageHeader(HttpHeaders.EMPTY),
            MultipartUtils.defaultBoundary(pageId3),
            entities("a", "Snapshot")
        ));
    }

    @Test
    @DisplayName("should produce a snapshot with multiple entities (buffered by ContentLength and MaxEntities)")
    void shouldProduceSnapshotContentLengthAndMaxEntities()
        throws ExecutionException, InterruptedException {

        List<Entity<SnapshotEntityHeader>> entities =
            entities("Hello World!", "Test", "of", "a", "Snapshot");
        Flux<Entity<SnapshotEntityHeader>> entityFlow = Flux.fromIterable(entities);
        when(pageIdProvider.newPageId()).thenReturn(pageId1, pageId2, pageId3, pageId4);
        when(snapshotPageUrlBuilder.pageUrl(id, pageId1)).thenReturn(page1Url);
        when(snapshotPageUrlBuilder.pageUrl(id, pageId2)).thenReturn(page2Url);
        when(snapshotPageUrlBuilder.pageUrl(id, pageId3)).thenReturn(page3Url);
        when(snapshotPageRepository.save(eq(id), any(), any()))
            .thenReturn(CompletableFuture.supplyAsync(() -> null));
        SnapshotProducer snapshotProducer = newSnapshotProducer(10L, 3L);

        CompletionStage<SnapshotIndex> produce =
            snapshotProducer.produce(FlowAdapters.toFlowPublisher(entityFlow));

        SnapshotIndex snapshotIndex = produce.toCompletableFuture().get();
        assertThat(snapshotIndex.pages()).containsExactly(page1Url, page2Url, page3Url);
        assertThat(snapshotIndex.id()).isEqualTo(id);
        assertThat(snapshotIndex.createdAt()).isEqualTo(createdAt);

        verify(snapshotPageRepository).save(id, pageId1, new Page<>(
            new SnapshotPageHeader(HttpHeaders.EMPTY),
            MultipartUtils.defaultBoundary(pageId1),
            entities("Hello World!")
        ));
        verify(snapshotPageRepository).save(id, pageId2, new Page<>(
            new SnapshotPageHeader(HttpHeaders.EMPTY),
            MultipartUtils.defaultBoundary(pageId2),
            entities("Test", "of", "a")
        ));
        verify(snapshotPageRepository).save(id, pageId3, new Page<>(
            new SnapshotPageHeader(HttpHeaders.EMPTY),
            MultipartUtils.defaultBoundary(pageId3),
            entities("Snapshot")
        ));
    }

    @Test
    @DisplayName("should conserve entity headers")
    void shouldConverseEntityHeaders()
        throws ExecutionException, InterruptedException {

        Entity<SnapshotEntityHeader> entity1 = entityWithHeaders(
            new SnapshotEntityHeader(
                HttpHeaders.of(
                    HttpHeader.contentLength(123),
                    HttpHeader.contentType(ContentType.of("application/json"))
                )),
            "Hello"
        );
        Entity<SnapshotEntityHeader> entity2 = entityWithHeaders(
            new SnapshotEntityHeader(
                HttpHeaders.of(
                    HttpHeader.contentLength(456),
                    HttpHeader.contentType(ContentType.of("application/xml"))
                )),
            "World"
        );

        Flux<Entity<SnapshotEntityHeader>> entityFlow = Flux.fromIterable(List.of(entity1, entity2));

        when(pageIdProvider.newPageId()).thenReturn(pageId1, pageId2);
        when(snapshotPageUrlBuilder.pageUrl(id, pageId1)).thenReturn(page1Url);
        when(snapshotPageUrlBuilder.pageUrl(id, pageId2)).thenReturn(page2Url);
        when(snapshotPageRepository.save(eq(id), any(), any()))
            .thenReturn(CompletableFuture.supplyAsync(() -> null));
        SnapshotProducer snapshotProducer = newSnapshotProducer(MAX_BYTES_PER_PAGE);

        snapshotProducer.produce(FlowAdapters.toFlowPublisher(entityFlow)).toCompletableFuture().get();

        verify(snapshotPageRepository).save(id, pageId1,
            new Page<>(new SnapshotPageHeader(HttpHeaders.EMPTY),
                MultipartUtils.defaultBoundary(pageId1),
                List.of(
                    entityWithHeaders(new SnapshotEntityHeader(
                        HttpHeaders.of(
                            HttpHeader.contentLength(123),
                            HttpHeader.contentType(ContentType.of("application/json"))
                        )), "Hello"
                    )
                )
            )
        );
        verify(snapshotPageRepository).save(id, pageId2,
            new Page<>(new SnapshotPageHeader(HttpHeaders.EMPTY),
                MultipartUtils.defaultBoundary(pageId2),
                List.of(
                    entityWithHeaders(new SnapshotEntityHeader(
                        HttpHeaders.of(
                            HttpHeader.contentLength(456),
                            HttpHeader.contentType(ContentType.of("application/xml"))
                        )), "World"
                    )
                )
            )
        );
    }

    private List<Entity<SnapshotEntityHeader>> entities(String... bodies) {
        return Arrays
            .stream(bodies)
            .map(body -> entityWithHeaders(new SnapshotEntityHeader(), body))
            .collect(Collectors.toUnmodifiableList());
    }

    private Entity<SnapshotEntityHeader> entityWithHeaders(SnapshotEntityHeader snapshotEntityHeader,
                                                           String body) {
        return new Entity<>(snapshotEntityHeader, Body.fromUtf8(body));
    }
}
