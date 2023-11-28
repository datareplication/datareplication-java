package io.datareplication.consumer.snapshot;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import io.datareplication.consumer.Authorization;
import io.datareplication.consumer.ConsumerException;
import io.datareplication.consumer.HttpException;
import io.datareplication.model.Body;
import io.datareplication.model.BodyTestUtil;
import io.datareplication.model.ContentType;
import io.datareplication.model.Entity;
import io.datareplication.model.HttpHeader;
import io.datareplication.model.HttpHeaders;
import io.datareplication.model.Url;
import io.datareplication.model.snapshot.SnapshotEntityHeader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import reactor.adapter.JdkFlowAdapter;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.IOException;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

class SnapshotConsumerIntegrationTest {
    private static final String PAGE_CONTENT_TYPE = "multipart/mixed; boundary=<random-boundary>";
    private static final String USERNAME = "snapshot-test-user";
    private static final String PASSWORD = "snapshot-test-password";
    private static final Authorization AUTH = Authorization.basic(USERNAME, PASSWORD);
    private static final String USER_AGENT = "snapshot-consumer-integration-test";

    private Url validSnapshotUrl;
    private Url onePageMissingSnapshotUrl;
    private Url allPagesMissingSnapshotUrl;

    @RegisterExtension
    static final WireMockExtension WM = WireMockExtension
        .newInstance()
        .options(wireMockConfig().port(8443))
        .build();

    @BeforeEach
    void setUp() {
        WM.stubFor(
            get("/index.json")
                .withBasicAuth(USERNAME, PASSWORD)
                .withHeader("User-Agent", equalTo(USER_AGENT))
                .willReturn(aResponse().withBodyFile("snapshot/index.json")
                ));
        WM.stubFor(
            get("/1.content.multipart")
                .withBasicAuth(USERNAME, PASSWORD)
                .withHeader("User-Agent", equalTo(USER_AGENT))
                .willReturn(aResponse()
                                .withHeader("Content-Type", PAGE_CONTENT_TYPE)
                                .withBodyFile("snapshot/1.content.multipart")
                ));
        WM.stubFor(
            get("/2.content.multipart")
                .withBasicAuth(USERNAME, PASSWORD)
                .withHeader("User-Agent", equalTo(USER_AGENT))
                .willReturn(aResponse()
                                .withHeader("Content-Type", PAGE_CONTENT_TYPE)
                                .withBodyFile("snapshot/2.content.multipart")
                ));
        WM.stubFor(
            get("/3.content.multipart")
                .withBasicAuth(USERNAME, PASSWORD)
                .withHeader("User-Agent", equalTo(USER_AGENT))
                .willReturn(aResponse()
                                .withHeader("Content-Type", PAGE_CONTENT_TYPE)
                                .withBodyFile("snapshot/3.content.multipart")
                ));
        validSnapshotUrl = Url.of(WM.url("/index.json"));

        WM.stubFor(get("/not-found-1.content.multipart").willReturn(aResponse().withStatus(404)));
        WM.stubFor(get("/not-found-2.content.multipart").willReturn(aResponse().withStatus(404)));
        WM.stubFor(get("/not-found-3.content.multipart").willReturn(aResponse().withStatus(404)));

        WM.stubFor(
            get("/onePageMissingIndex.json")
                .withBasicAuth(USERNAME, PASSWORD)
                .withHeader("User-Agent", equalTo(USER_AGENT))
                .willReturn(aResponse()
                                .withBodyFile("snapshot/onePageMissingIndex.json")
                ));
        onePageMissingSnapshotUrl = Url.of(WM.url("/onePageMissingIndex.json"));

        WM.stubFor(
            get("/allPagesMissingIndex.json")
                .withBasicAuth(USERNAME, PASSWORD)
                .withHeader("User-Agent", equalTo(USER_AGENT))
                .willReturn(aResponse()
                                .withBodyFile("snapshot/allPagesMissingIndex.json")
                ));
        allPagesMissingSnapshotUrl = Url.of(WM.url("/allPagesMissingIndex.json"));
    }

    @Test
    void shouldConsumeSnapshot() {
        SnapshotConsumer consumer = SnapshotConsumer
            .builder()
            .authorization(() -> AUTH)
            .additionalHeaders(HttpHeader.of("user-agent", USER_AGENT))
            .build();

        final var entities = Mono
            .fromCompletionStage(consumer.loadSnapshotIndex(validSnapshotUrl))
            .flatMapMany(snapshotIndex -> JdkFlowAdapter.flowPublisherToFlux(consumer.streamEntities(snapshotIndex)))
            .collectList()
            .single()
            .block();

        assertThat(entities)
            .usingRecursiveFieldByFieldElementComparator(BodyTestUtil.bodyContentsComparator())
            .containsExactlyInAnyOrder(
                new Entity<>(
                    new SnapshotEntityHeader(HttpHeaders.of(
                        HttpHeader.of("Content-Disposition", "inline"),
                        HttpHeader.of("Content-Transfer-Encoding", "8bit"),
                        HttpHeader.of("Last-Modified", "Thu, 5 Oct 2023 03:00:11 GMT"),
                        HttpHeader.of("Content-Length", "5")
                    )),
                    Body.fromUtf8("hello", ContentType.of("text/plain"))),
                new Entity<>(
                    new SnapshotEntityHeader(HttpHeaders.of(
                        HttpHeader.of("Content-Disposition", "inline"),
                        HttpHeader.of("Content-Transfer-Encoding", "8bit"),
                        HttpHeader.of("Last-Modified", "Thu, 5 Oct 2023 03:00:12 GMT"),
                        HttpHeader.of("Content-Length", "5")
                    )),
                    Body.fromUtf8("world", ContentType.of("text/plain"))),
                new Entity<>(
                    new SnapshotEntityHeader(HttpHeaders.of(
                        HttpHeader.of("Content-Disposition", "inline"),
                        HttpHeader.of("Content-Transfer-Encoding", "8bit"),
                        HttpHeader.of("Last-Modified", "Thu, 5 Oct 2023 03:00:13 GMT"),
                        HttpHeader.of("Content-Length", "1")
                    )),
                    Body.fromUtf8("I", ContentType.of("text/plain"))),
                new Entity<>(
                    new SnapshotEntityHeader(HttpHeaders.of(
                        HttpHeader.of("Content-Disposition", "inline"),
                        HttpHeader.of("Content-Transfer-Encoding", "8bit"),
                        HttpHeader.of("Last-Modified", "Thu, 5 Oct 2023 03:00:14 GMT"),
                        HttpHeader.of("Content-Length", "2")
                    )),
                    Body.fromUtf8("am", ContentType.of("text/plain"))),
                new Entity<>(
                    new SnapshotEntityHeader(HttpHeaders.of(
                        HttpHeader.of("Content-Disposition", "inline"),
                        HttpHeader.of("Content-Transfer-Encoding", "8bit"),
                        HttpHeader.of("Last-Modified", "Thu, 5 Oct 2023 03:00:15 GMT"),
                        HttpHeader.of("Content-Length", "1")
                    )),
                    Body.fromUtf8("a", ContentType.of("text/plain"))),
                new Entity<>(
                    new SnapshotEntityHeader(HttpHeaders.of(
                        HttpHeader.of("Content-Disposition", "inline"),
                        HttpHeader.of("Content-Transfer-Encoding", "8bit"),
                        HttpHeader.of("Last-Modified", "Thu, 5 Oct 2023 03:00:16 GMT"),
                        HttpHeader.of("Content-Length", "8")
                    )),
                    Body.fromUtf8("Snapshot", ContentType.of("text/plain")))
            );
    }

    @Test
    void shouldThrowException_whenOnePageIsMissing_delayErrors() {
        SnapshotConsumer consumer = SnapshotConsumer
            .builder()
            .authorization(AUTH)
            .additionalHeaders(HttpHeader.of("user-agent", USER_AGENT))
            .delayErrors(true)
            .networkConcurrency(1)
            .build();

        final var flux = Mono
            .fromCompletionStage(consumer.loadSnapshotIndex(onePageMissingSnapshotUrl))
            .flatMapMany(snapshotIndex -> JdkFlowAdapter.flowPublisherToFlux(consumer.streamEntities(snapshotIndex)))
            .map(entity -> {
                try {
                    return entity.body().toUtf8();
                } catch (IOException e) {
                    throw new AssertionError(e);
                }
            });

        StepVerifier
            .create(flux)
            .expectNext("hello", "world", "I", "am")
            .expectErrorMatches(new HttpException.ClientError(
                Url.of("http://localhost:8443/not-found-1.content.multipart"),
                404)::equals)
            .verify();
    }

    @Test
    void shouldThrowException_whenAllPagesAreMissing() {
        SnapshotConsumer consumer = SnapshotConsumer
            .builder()
            .authorization(AUTH)
            .additionalHeaders(HttpHeader.of("user-agent", USER_AGENT))
            .build();

        final var flux = Mono
            .fromCompletionStage(consumer.loadSnapshotIndex(allPagesMissingSnapshotUrl))
            .flatMapMany(snapshotIndex -> JdkFlowAdapter.flowPublisherToFlux(consumer.streamEntities(snapshotIndex)))
            .map(entity -> {
                try {
                    return entity.body().toUtf8();
                } catch (IOException e) {
                    throw new AssertionError(e);
                }
            });

        StepVerifier
            .create(flux)
            .expectNextCount(0)
            .expectError(HttpException.ClientError.class)
            .verify();
    }

    @Test
    void shouldThrowException_whenAllPagesAreMissing_delayErrors() {
        SnapshotConsumer consumer = SnapshotConsumer
            .builder()
            .authorization(AUTH)
            .additionalHeaders(HttpHeader.of("user-agent", USER_AGENT))
            .delayErrors(true)
            .build();

        final var flux = Mono
            .fromCompletionStage(consumer.loadSnapshotIndex(allPagesMissingSnapshotUrl))
            .flatMapMany(snapshotIndex -> JdkFlowAdapter.flowPublisherToFlux(consumer.streamEntities(snapshotIndex)))
            .map(entity -> {
                try {
                    return entity.body().toUtf8();
                } catch (IOException e) {
                    throw new AssertionError(e);
                }
            });

        StepVerifier
            .create(flux)
            .expectNextCount(0)
            .expectErrorSatisfies(exc -> {
                assertThat(exc)
                    .isInstanceOf(ConsumerException.CollectedErrors.class);
                final var collectedErrors = (ConsumerException.CollectedErrors) exc;
                assertThat(collectedErrors.exceptions())
                    .hasSize(3)
                    .allMatch(throwable -> throwable instanceof HttpException.ClientError);
            })
            .verify();
    }
}
