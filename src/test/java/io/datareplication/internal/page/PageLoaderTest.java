package io.datareplication.internal.page;

import com.github.mizosoft.methanol.Methanol;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import io.datareplication.consumer.HttpException;
import io.datareplication.consumer.StreamingPage;
import io.datareplication.model.ContentType;
import io.datareplication.model.HttpHeader;
import io.datareplication.model.HttpHeaders;
import io.datareplication.model.Url;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.reactivestreams.FlowAdapters;

import java.net.http.HttpClient;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static org.assertj.core.api.Assertions.assertThat;

class PageLoaderTest {
    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    @RegisterExtension
    static final WireMockExtension wireMock = WireMockExtension
        .newInstance()
        .build();

    private final HttpClient httpClient = Methanol.newBuilder()
        .requestTimeout(Duration.ofSeconds(3))
        .headersTimeout(Duration.ofSeconds(3))
        .readTimeout(Duration.ofSeconds(3))
        .autoAcceptEncoding(true)
        .build();
    private final PageLoader pageLoader = new PageLoader(httpClient);

    @Test
    void shouldDownloadAndParseMultipartPage() throws ExecutionException, InterruptedException {
        wireMock.stubFor(
            get("/page.multipart")
                .willReturn(
                    aResponse()
                        .withBodyFile("snapshot/1.content.multipart")
                        .withHeader("Content-Type", "multipart/mixed; boundary=<random-boundary>")
                        .withHeader("Test-Header", "value1", "value2")));

        final StreamingPage<HttpHeaders, HttpHeaders> page = pageLoader
            .load(Url.of(wireMock.url("/page.multipart")))
            .toCompletableFuture()
            .get();

        assertThat(page.header()).contains(
            HttpHeader.of("content-type", "multipart/mixed; boundary=<random-boundary>"),
            HttpHeader.of("test-header", List.of("value1", "value2"))
        );

        final List<StreamingPage.Chunk<HttpHeaders>> chunks = Flowable
            .fromPublisher(FlowAdapters.toPublisher(page))
            .toList()
            .blockingGet();
        assertThat(chunks).containsExactly(
            StreamingPage.Chunk.header(
                HttpHeaders.of(
                    HttpHeader.of("Content-Disposition", "inline"),
                    HttpHeader.of("Content-Transfer-Encoding", "8bit"),
                    HttpHeader.of("Last-Modified", "Thu, 5 Oct 2023 03:00:11 GMT"),
                    HttpHeader.of("Content-Length", "5")
                ),
                ContentType.of("text/plain")
            ),
            StreamingPage.Chunk.bodyChunk(ByteBuffer.wrap("hello".getBytes(StandardCharsets.UTF_8))),
            StreamingPage.Chunk.bodyEnd(),
            StreamingPage.Chunk.header(
                HttpHeaders.of(
                    HttpHeader.of("Content-Disposition", "inline"),
                    HttpHeader.of("Content-Transfer-Encoding", "8bit"),
                    HttpHeader.of("Last-Modified", "Thu, 5 Oct 2023 03:00:12 GMT"),
                    HttpHeader.of("Content-Length", "5")
                ),
                ContentType.of("text/plain")
            ),
            StreamingPage.Chunk.bodyChunk(ByteBuffer.wrap("world".getBytes(StandardCharsets.UTF_8))),
            StreamingPage.Chunk.bodyEnd()
        );
    }

    @Test
    void shouldReturnHttpException_whenHttp404() {
        wireMock.stubFor(
            get("/page.multipart")
                .willReturn(
                    aResponse()
                        .withStatus(404)
                        .withBody("this is not the url you're looking for")
                ));

        final CompletionStage<StreamingPage<HttpHeaders, HttpHeaders>> result = pageLoader
            .load(Url.of(wireMock.url("/page.multipart")));

        assertThat(result)
            .failsWithin(TIMEOUT)
            .withThrowableThat()
            .withCause(new HttpException.ClientError(404));
    }

    @Test
    void shouldReturnHttpException_whenHttp500() {
        wireMock.stubFor(
            get("/page.multipart")
                .willReturn(
                    aResponse()
                        .withStatus(500)
                        .withBody("oops")
                ));

        final CompletionStage<StreamingPage<HttpHeaders, HttpHeaders>> result = pageLoader
            .load(Url.of(wireMock.url("/page.multipart")));

        assertThat(result)
            .failsWithin(TIMEOUT)
            .withThrowableThat()
            .withCause(new HttpException.ServerError(500));
    }
}
