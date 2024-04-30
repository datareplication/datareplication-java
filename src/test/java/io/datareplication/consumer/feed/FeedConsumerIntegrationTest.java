package io.datareplication.consumer.feed;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import io.datareplication.consumer.Authorization;
import io.datareplication.model.BodyTestUtil;
import io.datareplication.model.HttpHeader;
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
import java.time.Instant;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.head;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static com.github.tomakehurst.wiremock.http.HttpHeader.httpHeader;
import static io.datareplication.model.feed.OperationType.DELETE;
import static io.datareplication.model.feed.OperationType.PUT;
import static java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME;
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
    void shouldConsumeFeed_startFromBeginning() {
        FeedConsumer consumer = FeedConsumer
            .builder()
            .authorization(() -> AUTH)
            .additionalHeaders(HttpHeader.of("user-agent", USER_AGENT))
            .build();
        StartFrom startFrom = StartFrom.beginning();

        final var entities = Flux
            .concat(FlowAdapters.toPublisher(consumer.streamEntities(validFeedUrl, startFrom)))
            .map(entity -> {
                try {
                    return new Pair(entity.header().operationType(), entity.body().toUtf8());
                } catch (IOException e) {
                    return fail(e);
                }
            })
            .collectList()
            .block();

        assertThat(entities)
            .hasSize(9)
            .usingRecursiveFieldByFieldElementComparator(BodyTestUtil.bodyContentsComparator())
            .containsExactly(
                new Pair(PUT, "only consumable with \"StartFrom.beginning()\""),
                new Pair(PUT, "hello"),
                new Pair(PUT, "world"),
                new Pair(PUT, "I"),
                new Pair(PUT, "am"),
                new Pair(PUT, "a"),
                new Pair(PUT, "snapshot"),
                new Pair(DELETE, "snapshot"),
                new Pair(PUT, "feed")
            );
    }

    @Test
    void shouldConsumeFeed_startFromTimestamp() {
        FeedConsumer consumer = FeedConsumer
            .builder()
            .authorization(() -> AUTH)
            .additionalHeaders(HttpHeader.of("user-agent", USER_AGENT))
            .build();
        StartFrom startFrom = StartFrom.timestamp(
            Instant.from(RFC_1123_DATE_TIME.parse("Mon, 27 Nov 2023 00:00:01 GMT"))
        );

        final var entities = Flux
            .concat(FlowAdapters.toPublisher(consumer.streamEntities(validFeedUrl, startFrom)))
            .map(entity -> {
                try {
                    return new Pair(entity.header().operationType(), entity.body().toUtf8());
                } catch (IOException e) {
                    return fail(e);
                }
            })
            .collectList()
            .block();

        assertThat(entities)
            .hasSize(8)
            .containsExactly(
                new Pair(PUT, "hello"),
                new Pair(PUT, "world"),
                new Pair(PUT, "I"),
                new Pair(PUT, "am"),
                new Pair(PUT, "a"),
                new Pair(PUT, "snapshot"),
                new Pair(DELETE, "snapshot"),
                new Pair(PUT, "feed")
            );
    }

    @Test
    void shouldConsumeFeed_startFromContentId() {
        FeedConsumer consumer = FeedConsumer
            .builder()
            .authorization(() -> AUTH)
            .additionalHeaders(HttpHeader.of("user-agent", USER_AGENT))
            .build();

        StartFrom startFrom = StartFrom.contentId(
            ContentId.of("<1-B@random-content-id>"),
            Instant.from(RFC_1123_DATE_TIME.parse("Mon, 27 Nov 2023 03:10:00 GMT"))
        );
        final var entities = Flux
            .concat(FlowAdapters.toPublisher(consumer.streamEntities(validFeedUrl, startFrom)))
            .map(entity -> {
                try {
                    return new Pair(entity.header().operationType(), entity.body().toUtf8());
                } catch (IOException e) {
                    return fail(e);
                }
            })
            .collectList()
            .block();

        assertThat(entities)
            .hasSize(6)
            .containsExactly(
                new Pair(PUT, "I"),
                new Pair(PUT, "am"),
                new Pair(PUT, "a"),
                new Pair(PUT, "snapshot"),
                new Pair(DELETE, "snapshot"),
                new Pair(PUT, "feed")
            );
    }

    private void stubForContent0(String testUrl) {
        var headers = new com.github.tomakehurst.wiremock.http.HttpHeaders(
            httpHeader("Content-Type", PAGE_CONTENT_TYPE),
            httpHeader("Last-Modified", "Mon, 27 Nov 2023 00:00:00 GMT"),
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
