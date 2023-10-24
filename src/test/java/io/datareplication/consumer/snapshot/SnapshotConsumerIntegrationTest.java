package io.datareplication.consumer.snapshot;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import io.datareplication.model.Entity;
import io.datareplication.model.Url;
import io.datareplication.model.snapshot.SnapshotEntityHeader;
import io.datareplication.model.snapshot.SnapshotIndex;
import io.reactivex.rxjava3.core.Flowable;
import lombok.NonNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.reactivestreams.FlowAdapters;

import java.util.concurrent.ExecutionException;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

class SnapshotConsumerIntegrationTest {
    private Url snapshotUrl;

    @RegisterExtension
    final WireMockExtension wm = WireMockExtension.newInstance()
        .options(wireMockConfig().httpsPort(8443))
        .build();

    @BeforeEach
    void setUp() {
        wm.stubFor(get("/index.json")
            .willReturn(aResponse().withBodyFile("snapshot/index.json")));
        wm.stubFor(get("/1.content.multipart")
            .willReturn(aResponse().withBodyFile("snapshot/1.content.multipart")));
        wm.stubFor(get("/2.content.multipart")
            .willReturn(aResponse().withBodyFile("snapshot/2.content.multipart")));
        wm.stubFor(get("/3.content.multipart")
            .willReturn(aResponse().withBodyFile("snapshot/3.content.multipart")));
        snapshotUrl = Url.of(wm.getRuntimeInfo().getHttpBaseUrl() + "/index.json");
    }

    @Test
    void shouldConsumeSnapshot() throws ExecutionException, InterruptedException {
        SnapshotConsumer consumer = SnapshotConsumer
            .builder()
            // TODO: Additional configuration
            .build();

        SnapshotIndex snapshotIndex = consumer.loadSnapshotIndex(snapshotUrl).toCompletableFuture().get();
        Flowable<@NonNull Entity<@NonNull SnapshotEntityHeader>> entityFlowable =
            Flowable.fromPublisher(FlowAdapters.toPublisher(consumer.streamEntities(snapshotIndex)));

        entityFlowable
            .map(entity -> entity.body().toUtf8())
            .test()
            .assertValues("Hello", "World", "I", "am", "a", "Snapshot");
    }
}
