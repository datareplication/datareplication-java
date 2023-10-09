package io.datareplication.consumer.snapshot;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import io.datareplication.model.Url;
import io.datareplication.model.snapshot.SnapshotIndex;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

class SnapshotConsumerIntegrationTest {
    private Url snapshotUrl;

    @RegisterExtension
    static WireMockExtension wm = WireMockExtension.newInstance()
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
        List<String> snapshotEntities = List.of("Hello", "World", "I", "am", "a", "Snapshot");
        SnapshotEntitySubscriber subscriber = new SnapshotEntitySubscriber();

        SnapshotConsumer consumer = SnapshotConsumer
            .builder()
            // TODO: Additional configuration
            .build();

        SnapshotIndex snapshotIndex = consumer.loadSnapshotIndex(snapshotUrl).toCompletableFuture().get();

        consumer.streamEntities(snapshotIndex).subscribe(subscriber);
        await().atMost(5, TimeUnit.SECONDS).until(subscriber::hasCompleted);
        assertThat(subscriber.getConsumedEntities()).isEqualTo(snapshotEntities);
    }
}
