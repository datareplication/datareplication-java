package io.datareplication;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import io.datareplication.consumer.snapshot.SnapshotConsumer;
import io.datareplication.model.Body;
import io.datareplication.model.Entity;
import io.datareplication.model.HttpHeaders;
import io.datareplication.model.PageId;
import io.datareplication.model.Url;
import io.datareplication.model.snapshot.SnapshotEntityHeader;
import io.datareplication.model.snapshot.SnapshotId;
import io.datareplication.model.snapshot.SnapshotIndex;
import io.datareplication.producer.snapshot.SnapshotPageUrlBuilder;
import io.datareplication.producer.snapshot.SnapshotProducer;
import io.datareplication.producer.snapshot.testhelper.SnapshotIndexInMemoryRepository;
import io.datareplication.producer.snapshot.testhelper.SnapshotPageInMemoryRepository;
import lombok.NonNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.exceptions.misusing.UnfinishedStubbingException;
import org.reactivestreams.FlowAdapters;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

public class SnapshotAcceptanceTest {
    @RegisterExtension
    static WireMockExtension wm = WireMockExtension.newInstance()
        .options(wireMockConfig().httpsPort(8443))
        .build();

    @Test
    void shouldPublishAndConsumeSnapshot() throws ExecutionException, InterruptedException, IOException {
        //region Produce Snapshot
        Flux<Entity<SnapshotEntityHeader>> entityFlow = Flux
            .just("Hello", "World", "I", "am", "a", "Snapshot")
            .map(this::toSnapshotEntity);
        SnapshotIndexInMemoryRepository snapshotIndexRepository = new SnapshotIndexInMemoryRepository();
        SnapshotPageInMemoryRepository snapshotPageRepository = new SnapshotPageInMemoryRepository();
        SnapshotPageUrlBuilder snapshotPageUrlBuilder = new SnapshotPageUrlBuilder() {
            @Override
            public @NonNull Url pageUrl(@NonNull final SnapshotId snapshotId, @NonNull final PageId pageId) {
                return Url.of(wm.url("/" + snapshotId.value() + "/" + pageId.value()));
            }
        };
        SnapshotProducer snapshotProducer = SnapshotProducer
            .builder()
            .maxEntriesPerPage(2)
            .maxBytesPerPage(5)
            .build(snapshotIndexRepository, snapshotPageRepository, snapshotPageUrlBuilder);
        SnapshotIndex producedSnapshotIndex = snapshotProducer
            .produce(FlowAdapters.toFlowPublisher(entityFlow))
            .toCompletableFuture()
            .get();
        wiremockStubsFor(producedSnapshotIndex.id(), snapshotPageRepository, snapshotIndexRepository);
        //endregion
        //region Consume Snapshot
        SnapshotConsumer snapshotConsumer = SnapshotConsumer
            .builder()
            // TODO: Additional configuration
            .build();
        Flux<@NonNull Entity<@NonNull SnapshotEntityHeader>> entityFlowable =
            Flux.from(
                FlowAdapters.toPublisher(snapshotConsumer.streamEntities(producedSnapshotIndex))
            );
        //endregion
        //region Assert SnapshotIndex
        var snapshotIndexFromUrl = snapshotConsumer
            .loadSnapshotIndex(snapshotUrl(producedSnapshotIndex))
            .toCompletableFuture()
            .get();
        assertThat(producedSnapshotIndex).isEqualTo(snapshotIndexFromUrl);
        //endregion
        //region Assert consumed entities
        var entities = entityFlowable
            .map(entity -> {
                try {
                    return entity.body().toUtf8();
                } catch (IOException e) {
                    return fail(e);
                }
            })
            .toIterable();
        assertThat(entities).containsExactly("Hello", "World", "I", "am", "a", "Snapshot");
        //endregion
    }

    private void wiremockStubsFor(SnapshotId snapshotId,
                                  SnapshotPageInMemoryRepository snapshotPageRepository,
                                  SnapshotIndexInMemoryRepository snapshotIndexInMemoryRepository) throws IOException {
        Optional<SnapshotIndex> maybeSnapshotIndex = snapshotIndexInMemoryRepository.findBy(snapshotId);
        if (maybeSnapshotIndex.isEmpty()) {
            throw new UnfinishedStubbingException("SnapshotIndex not found for " + snapshotId.value());
        }
        SnapshotIndex snapshotIndex = maybeSnapshotIndex.get();
        wm.stubFor(get("/" + snapshotId)
            .willReturn(aResponse().withBodyFile(snapshotIndex.toJson().toUtf8())));
        for (Url url : snapshotIndex.pages()) {
            wm.stubFor(get(url.value()
                // Truncate to relative path
                .replace(wm.baseUrl(), ""))
                .willReturn(
                    aResponse().withBody(
                        snapshotPageRepository
                            .findBy(pageId(url, snapshotIndex.id()))
                            .orElse(Body.fromUtf8("404 Notfound"))
                            .toUtf8()
                    )
                )
            );
        }
    }

    private @NonNull Entity<@NonNull SnapshotEntityHeader> toSnapshotEntity(String content) {
        return new Entity<>(new SnapshotEntityHeader(HttpHeaders.EMPTY), Body.fromUtf8(content));
    }

    private @NonNull Url snapshotUrl(@NonNull SnapshotIndex index) {
        return Url.of(wm.url("/" + index.id().value()));
    }

    private @NonNull PageId pageId(@NonNull Url url, @NonNull SnapshotId snapshotId) {
        return PageId.of(url.value()
            // Extract PageId from Url
            .replace(wm.url("/" + snapshotId.value() + "/"), ""));
    }
}
