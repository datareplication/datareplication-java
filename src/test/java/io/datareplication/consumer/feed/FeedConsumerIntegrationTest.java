package io.datareplication.consumer.feed;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import io.datareplication.consumer.Authorization;
import io.datareplication.model.Body;
import io.datareplication.model.BodyTestUtil;
import io.datareplication.model.ContentType;
import io.datareplication.model.Entity;
import io.datareplication.model.HttpHeader;
import io.datareplication.model.HttpHeaders;
import io.datareplication.model.Timestamp;
import io.datareplication.model.Url;
import io.datareplication.model.feed.ContentId;
import io.datareplication.model.feed.FeedEntityHeader;
import io.datareplication.model.feed.OperationType;
import lombok.EqualsAndHashCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.reactivestreams.FlowAdapters;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.head;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static com.github.tomakehurst.wiremock.http.HttpHeader.httpHeader;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

class FeedConsumerIntegrationTest {
    private static final String PAGE_CONTENT_TYPE = "multipart/mixed; boundary=<random-boundary>";
    private static final String USERNAME = "feed-test-user";
    private static final String PASSWORD = "feed-test-password";
    private static final Authorization AUTH = Authorization.basic(USERNAME, PASSWORD);
    private static final String USER_AGENT = "feed-consumer-integration-test";
    private static final String FMT_LINK = "LINK; rel=%s";

    private Url validFeedUrl;

    @RegisterExtension
    static final WireMockExtension WM = WireMockExtension
        .newInstance()
        .options(wireMockConfig().port(8443))
        .build();

    @BeforeEach
    void setUp() {
        stubForContent3("/latest.multipart");
        stubForContent3("/3.content.multipart");
        stubForContent2("/2.content.multipart");
        stubForContent1("/1.content.multipart");
        stubForContent0("/0.content.multipart");

        validFeedUrl = Url.of(WM.url("/latest.multipart"));
    }

    @Test
    void shouldConsumeFeedFromBeginning() throws ExecutionException, InterruptedException {
        FeedConsumer consumer = FeedConsumer
            .builder()
            .authorization(() -> AUTH)
            .additionalHeaders(HttpHeader.of("user-agent", USER_AGENT))
            .build();

        final var entities = Flux.concat(FlowAdapters.toPublisher(
                consumer.streamEntities(validFeedUrl, StartFrom.beginning()))
            )
            .collectList()
            .toFuture()
            .get();

        assertThat(entities)
            .usingRecursiveFieldByFieldElementComparator(BodyTestUtil.bodyContentsComparator())
            .contains(
                new Entity<>(
                    new FeedEntityHeader(
                        Timestamp.fromRfc1123String("Mon, 27 Nov 2023 00:00:00 GMT"),
                        OperationType.PUT,
                        ContentId.of("<0-A@random-content-id>"),
                        HttpHeaders.of(
                            HttpHeader.of("Content-Disposition", "inline"),
                            HttpHeader.of("Content-Transfer-Encoding", "8bit"),
                            HttpHeader.of("Content-Length", "44")
                        )),
                    Body.fromUtf8("only consumable with \"StartFrom.beginning()\"", ContentType.of("text/plain"))),
                new Entity<>(
                    new FeedEntityHeader(
                        Timestamp.fromRfc1123String("Mon, 27 Nov 2023 02:41:07 GMT"),
                        OperationType.PUT,
                        ContentId.of("<0-B@random-content-id>"),
                        HttpHeaders.of(
                            HttpHeader.of("Content-Disposition", "inline"),
                            HttpHeader.of("Content-Transfer-Encoding", "8bit"),
                            HttpHeader.of("Content-Length", "70")
                        )),
                    Body.fromUtf8(
                        "First consumable entity, but in this case it's a delete operation-type",
                        ContentType.of("text/plain")
                    )
                ),
                new Entity<>(
                    new FeedEntityHeader(
                        Timestamp.fromRfc1123String("Mon, 27 Nov 2023 03:10:00 GMT"),
                        OperationType.PUT,
                        ContentId.of("<1-A@random-content-id>"),
                        HttpHeaders.of(
                            HttpHeader.of("Content-Disposition", "inline"),
                            HttpHeader.of("Content-Transfer-Encoding", "8bit"),
                            HttpHeader.of("Content-Length", "5")
                        )),
                    Body.fromUtf8("hello", ContentType.of("text/plain"))),
                new Entity<>(
                    new FeedEntityHeader(
                        Timestamp.fromRfc1123String("Mon, 27 Nov 2023 03:10:00 GMT"),
                        OperationType.PUT,
                        ContentId.of("<1-B@random-content-id>"),
                        HttpHeaders.of(
                            HttpHeader.of("Content-Disposition", "inline"),
                            HttpHeader.of("Content-Transfer-Encoding", "8bit"),
                            HttpHeader.of("Content-Length", "5")
                        )),
                    Body.fromUtf8("world", ContentType.of("text/plain"))),
                new Entity<>(
                    new FeedEntityHeader(
                        Timestamp.fromRfc1123String("Mon, 27 Nov 2023 03:10:00 GMT"),
                        OperationType.PUT,
                        ContentId.of("<2-A@random-content-id>"),
                        HttpHeaders.of(
                            HttpHeader.of("Content-Disposition", "inline"),
                            HttpHeader.of("Content-Transfer-Encoding", "8bit"),
                            HttpHeader.of("Content-Length", "1")
                        )),
                    Body.fromUtf8("I", ContentType.of("text/plain"))),
                new Entity<>(
                    new FeedEntityHeader(
                        Timestamp.fromRfc1123String("Mon, 27 Nov 2023 04:02:30 GMT"),
                        OperationType.PUT,
                        ContentId.of("<2-B@random-content-id>"),
                        HttpHeaders.of(
                            HttpHeader.of("Content-Disposition", "inline"),
                            HttpHeader.of("Content-Transfer-Encoding", "8bit"),
                            HttpHeader.of("Content-Length", "2")
                        )),
                    Body.fromUtf8("am", ContentType.of("text/plain"))),
                new Entity<>(
                    new FeedEntityHeader(
                        Timestamp.fromRfc1123String("Thu, 28 Nov 2023 01:00:00 GMT"),
                        OperationType.PUT,
                        ContentId.of("<3-A@random-content-id>"),
                        HttpHeaders.of(
                            HttpHeader.of("Content-Disposition", "inline"),
                            HttpHeader.of("Content-Transfer-Encoding", "8bit"),
                            HttpHeader.of("Content-Length", "1")
                        )),
                    Body.fromUtf8("a", ContentType.of("text/plain"))),
                new Entity<>(
                    new FeedEntityHeader(
                        Timestamp.fromRfc1123String("Thu, 28 Nov 2023 12:00:00 GMT"),
                        OperationType.PUT,
                        ContentId.of("<3-B@random-content-id>"),
                        HttpHeaders.of(
                            HttpHeader.of("Content-Disposition", "inline"),
                            HttpHeader.of("Content-Transfer-Encoding", "8bit"),
                            HttpHeader.of("Content-Length", "8")
                        )),
                    Body.fromUtf8("snapshot", ContentType.of("text/plain"))),
                new Entity<>(
                    new FeedEntityHeader(
                        Timestamp.fromRfc1123String("Thu, 28 Nov 2023 12:30:01 GMT"),
                        OperationType.DELETE,
                        ContentId.of("<3-C@random-content-id>"),
                        HttpHeaders.of(
                            HttpHeader.of("Content-Disposition", "inline"),
                            HttpHeader.of("Content-Transfer-Encoding", "8bit"),
                            HttpHeader.of("Content-Length", "8")
                        )),
                    Body.fromUtf8("snapshot", ContentType.of("text/plain"))),
                new Entity<>(
                    new FeedEntityHeader(
                        Timestamp.fromRfc1123String("Thu, 28 Nov 2023 18:30:00 GMT"),
                        OperationType.PUT,
                        ContentId.of("<3-D@random-content-id>"),
                        HttpHeaders.of(
                            HttpHeader.of("Content-Disposition", "inline"),
                            HttpHeader.of("Content-Transfer-Encoding", "8bit"),
                            HttpHeader.of("Content-Length", "8")
                        )),
                    Body.fromUtf8("feed", ContentType.of("text/plain")))
            );
    }

    @Test
    void shouldConsumeFeedFromTimestamp() throws ExecutionException, InterruptedException {
        FeedConsumer consumer = FeedConsumer
            .builder()
            .authorization(() -> AUTH)
            .additionalHeaders(HttpHeader.of("user-agent", USER_AGENT))
            .build();

        final var entities = Flux.concat(FlowAdapters.toPublisher(
                consumer.streamEntities(
                    validFeedUrl,
                    StartFrom.timestamp(Timestamp.fromRfc1123String("Mon, 27 Nov 2023 00:00:00 GMT"))
                )))
            .map(entity -> {
                try {
                    return new Pair(entity.header().operationType(), entity.body().toUtf8());
                } catch (IOException e) {
                    return fail(e);
                }
            })
            .collectList()
            .toFuture()
            .get();

        assertThat(entities).hasSize(9);
        assertThat(entities).containsExactly(
            new Pair(OperationType.PUT, "First consumable entity, but in this case it's a delete operation-type"),
            new Pair(OperationType.PUT, "hello"),
            new Pair(OperationType.PUT, "world"),
            new Pair(OperationType.PUT, "I"),
            new Pair(OperationType.PUT, "am"),
            new Pair(OperationType.PUT, "a"),
            new Pair(OperationType.PUT, "snapshot"),
            new Pair(OperationType.DELETE, "snapshot"),
            new Pair(OperationType.PUT, "feed")
        );
    }

    @Test
    void shouldConsumeFeedFromTimestampAndContentId() throws ExecutionException, InterruptedException {
        FeedConsumer consumer = FeedConsumer
            .builder()
            .authorization(() -> AUTH)
            .additionalHeaders(HttpHeader.of("user-agent", USER_AGENT))
            .build();

        final var entities = Flux.concat(FlowAdapters.toPublisher(
                consumer.streamEntities(
                    validFeedUrl,
                    StartFrom.contentId(
                        ContentId.of("0-B@random-content-id"),
                        Timestamp.fromRfc1123String("Mon, 27 Nov 2023 00:00:00 GMT")
                    )
                )))
            .map(entity -> {
                try {
                    return new Pair(entity.header().operationType(), entity.body().toUtf8());
                } catch (IOException e) {
                    return fail(e);
                }
            })
            .collectList()
            .toFuture()
            .get();

        assertThat(entities).hasSize(8);
        assertThat(entities).containsExactly(
            new Pair(OperationType.PUT, "hello"),
            new Pair(OperationType.PUT, "world"),
            new Pair(OperationType.PUT, "I"),
            new Pair(OperationType.PUT, "am"),
            new Pair(OperationType.PUT, "a"),
            new Pair(OperationType.PUT, "snapshot"),
            new Pair(OperationType.DELETE, "snapshot"),
            new Pair(OperationType.PUT, "feed")
        );
    }

    // TODO: IntegrationTest only for an error!?

    private void stubForContent0(String testUrl) {
        var headers = new com.github.tomakehurst.wiremock.http.HttpHeaders(
            httpHeader("Content-Type", PAGE_CONTENT_TYPE),
            httpHeader("Last-Modified", "Mon, 27 Nov 2023 02:41:07 GMT"),
            httpHeader(String.format(FMT_LINK, "self"), WM.url("0.content.multipart")),
            httpHeader(String.format(FMT_LINK, "prev"), WM.url("1.content.multipart"))
        );
        WM.stubFor(
            head(urlPathEqualTo(testUrl))
                .withBasicAuth(USERNAME, PASSWORD)
                .withHeader("User-Agent", equalTo(USER_AGENT))
                .willReturn(aResponse().withHeaders(headers)));
        WM.stubFor(
            get(testUrl)
                .withBasicAuth(USERNAME, PASSWORD)
                .withHeader("User-Agent", equalTo(USER_AGENT))
                .willReturn(aResponse().withHeaders(headers)
                    .withBodyFile("feed/1.content.multipart")));
    }

    private void stubForContent1(String testUrl) {
        var headers = new com.github.tomakehurst.wiremock.http.HttpHeaders(
            httpHeader("Content-Type", PAGE_CONTENT_TYPE),
            httpHeader("Last-Modified", "Mon, 27 Nov 2023 03:10:00 GMT"),
            httpHeader(String.format(FMT_LINK, "prev"), WM.url("0.content.multipart")),
            httpHeader(String.format(FMT_LINK, "self"), WM.url("1.content.multipart")),
            httpHeader(String.format(FMT_LINK, "prev"), WM.url("2.content.multipart"))
        );
        WM.stubFor(
            head(urlPathEqualTo(testUrl))
                .withBasicAuth(USERNAME, PASSWORD)
                .withHeader("User-Agent", equalTo(USER_AGENT))
                .willReturn(aResponse().withHeaders(headers)));
        WM.stubFor(
            get(testUrl)
                .withBasicAuth(USERNAME, PASSWORD)
                .withHeader("User-Agent", equalTo(USER_AGENT))
                .willReturn(aResponse().withHeaders(headers)
                    .withBodyFile("feed/1.content.multipart")));
    }

    private void stubForContent2(String testUrl) {
        var headers = new com.github.tomakehurst.wiremock.http.HttpHeaders(
            httpHeader("Content-Type", PAGE_CONTENT_TYPE),
            httpHeader("Last-Modified", "Mon, 27 Nov 2023 04:02:30 GMT"),
            httpHeader(String.format(FMT_LINK, "prev"), WM.url("1.content.multipart")),
            httpHeader(String.format(FMT_LINK, "self"), WM.url("2.content.multipart")),
            httpHeader(String.format(FMT_LINK, "prev"), WM.url("3.content.multipart"))
        );
        WM.stubFor(
            head(urlPathEqualTo(testUrl))
                .withBasicAuth(USERNAME, PASSWORD)
                .withHeader("User-Agent", equalTo(USER_AGENT))
                .willReturn(aResponse().withHeaders(headers)));
        WM.stubFor(
            get(testUrl)
                .withBasicAuth(USERNAME, PASSWORD)
                .withHeader("User-Agent", equalTo(USER_AGENT))
                .willReturn(aResponse().withHeaders(headers)
                    .withBodyFile("feed/2.content.multipart")));
    }

    private void stubForContent3(String testUrl) {
        var headers = new com.github.tomakehurst.wiremock.http.HttpHeaders(
            httpHeader("Content-Type", PAGE_CONTENT_TYPE),
            httpHeader("Last-Modified", "Thu, 28 Nov 2023 18:30:00 GMT"),
            httpHeader(String.format(FMT_LINK, "prev"), WM.url("2.content.multipart")),
            httpHeader(String.format(FMT_LINK, "self"), WM.url("3.content.multipart"))

        );
        WM.stubFor(
            head(urlPathEqualTo(testUrl))
                .withBasicAuth(USERNAME, PASSWORD)
                .withHeader("User-Agent", equalTo(USER_AGENT))
                .willReturn(aResponse().withHeaders(headers)));
        WM.stubFor(
            get(testUrl)
                .withBasicAuth(USERNAME, PASSWORD)
                .withHeader("User-Agent", equalTo(USER_AGENT))
                .willReturn(aResponse().withHeaders(headers)
                    .withBodyFile("feed/3.content.multipart")));
    }

    @EqualsAndHashCode(callSuper = false)
    private static class Pair {
        private final OperationType operationType;
        private final String message;

        Pair(OperationType operationType, String message) {
            this.operationType = operationType;
            this.message = message;
        }
    }
}
