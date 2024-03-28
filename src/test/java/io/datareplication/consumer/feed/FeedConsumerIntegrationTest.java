package io.datareplication.consumer.feed;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import io.datareplication.consumer.Authorization;
import io.datareplication.model.BodyTestUtil;
import io.datareplication.model.HttpHeader;
import io.datareplication.model.Timestamp;
import io.datareplication.model.Url;
import io.datareplication.model.feed.ContentId;
import io.datareplication.model.feed.OperationType;
import lombok.Data;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.reactivestreams.FlowAdapters;
import reactor.core.publisher.Flux;

import java.io.IOException;

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
    private static final String FMT_LINK_REL_VALUE = "%s; rel=%s";

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
    void shouldConsumeFeedFromBeginning() {
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
            .block();

        assertThat(entities).hasSize(10);
        assertThat(entities)
            .usingRecursiveFieldByFieldElementComparator(BodyTestUtil.bodyContentsComparator())
            .containsExactly(
                new Pair(OperationType.PUT, "only consumable with \"StartFrom.beginning()\""),
                new Pair(OperationType.DELETE, "First consumable entity, but in this case it's a delete operation-type"),
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
    void shouldConsumeFeedFromTimestamp() {
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
            .block();

        assertThat(entities).hasSize(9);
        assertThat(entities).containsExactly(
            new Pair(OperationType.DELETE, "First consumable entity, but in this case it's a delete operation-type"),
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
    void shouldConsumeFeedFromTimestampAndContentId() {
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
            .block();

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

    private void stubForContent0(String testUrl) {
        var headers = new com.github.tomakehurst.wiremock.http.HttpHeaders(
            httpHeader("Content-Type", PAGE_CONTENT_TYPE),
            httpHeader("Last-Modified", "Mon, 27 Nov 2023 02:41:07 GMT"),
            httpHeader("link", String.format(FMT_LINK_REL_VALUE, WM.url("0.content.multipart"), "self")),
            httpHeader("link", String.format(FMT_LINK_REL_VALUE, WM.url("1.content.multipart"), "next"))
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
                    .withBodyFile("feed/0.content.multipart")));
    }

    private void stubForContent1(String testUrl) {
        var headers = new com.github.tomakehurst.wiremock.http.HttpHeaders(
            httpHeader("Content-Type", PAGE_CONTENT_TYPE),
            httpHeader("Last-Modified", "Mon, 27 Nov 2023 03:10:00 GMT"),
            httpHeader("link", String.format(FMT_LINK_REL_VALUE, WM.url("0.content.multipart"), "prev")),
            httpHeader("link", String.format(FMT_LINK_REL_VALUE, WM.url("1.content.multipart"), "self")),
            httpHeader("link", String.format(FMT_LINK_REL_VALUE, WM.url("2.content.multipart"), "next"))
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
            httpHeader("link", String.format(FMT_LINK_REL_VALUE, WM.url("1.content.multipart"), "prev")),
            httpHeader("link", String.format(FMT_LINK_REL_VALUE, WM.url("2.content.multipart"), "self")),
            httpHeader("link", String.format(FMT_LINK_REL_VALUE, WM.url("3.content.multipart"), "next"))
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
            httpHeader("Last-Modified", "Tue, 28 Nov 2023 18:30:00 GMT"),
            httpHeader("link", String.format(FMT_LINK_REL_VALUE, WM.url("2.content.multipart"), "prev")),
            httpHeader("link", String.format(FMT_LINK_REL_VALUE, WM.url("3.content.multipart"), "self"))
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

    @Data
    private static class Pair {
        private final OperationType operationType;
        private final String message;
    }
}
