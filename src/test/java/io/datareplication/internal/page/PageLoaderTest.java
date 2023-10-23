package io.datareplication.internal.page;

import com.github.mizosoft.methanol.Methanol;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import io.datareplication.consumer.PageFormatException;
import io.datareplication.consumer.StreamingPage;
import io.datareplication.internal.http.HttpClient;
import io.datareplication.internal.multipart.MultipartException;
import io.datareplication.model.ContentType;
import io.datareplication.model.HttpHeader;
import io.datareplication.model.HttpHeaders;
import io.datareplication.model.Url;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.reactivestreams.FlowAdapters;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static org.assertj.core.api.Assertions.assertThat;

class PageLoaderTest {
    @RegisterExtension
    static final WireMockExtension wireMock = WireMockExtension
        .newInstance()
        .build();

    private final HttpClient httpClient = new HttpClient(
        Methanol.newBuilder()
            .requestTimeout(Duration.ofSeconds(3))
            .headersTimeout(Duration.ofSeconds(3))
            .readTimeout(Duration.ofSeconds(3))
            .build(),
        0
    );
    private final PageLoader pageLoader = new PageLoader(httpClient);

    @Test
    void shouldDownloadAndParseMultipartPage() {
        wireMock.stubFor(
            get("/page.multipart")
                .willReturn(
                    aResponse()
                        .withBodyFile("snapshot/1.content.multipart")
                        .withHeader("Content-Type", "multipart/mixed; boundary=<random-boundary>")
                        .withHeader("Test-Header", "value1", "value2")));

        final StreamingPage<HttpHeaders, HttpHeaders> page = pageLoader
            .load(Url.of(wireMock.url("/page.multipart")))
            .blockingGet();

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
    void shouldThrowPageFormatException_whenNoContentTypeHeader() throws InterruptedException {
        wireMock.stubFor(
            get("/page.multipart")
                .willReturn(
                    aResponse()
                        .withBodyFile("snapshot/1.content.multipart")
                        .withHeader("Content-Type-oops-no", "multipart/mixed; boundary=<random-boundary>"))
        );

        final Single<StreamingPage<HttpHeaders, HttpHeaders>> result = pageLoader
            .load(Url.of(wireMock.url("/page.multipart")));

        result
            .test()
            .await()
            .assertError(new PageFormatException.MissingContentTypeHeader());
    }

    @Test
    void shouldThrowPageFormatException_whenNotMultipart() throws InterruptedException {
        wireMock.stubFor(
            get("/page.multipart")
                .willReturn(
                    aResponse()
                        .withBodyFile("snapshot/1.content.multipart")
                        .withHeader("Content-Type", "text/plain; boundary=<random-boundary>"))
        );

        final Single<StreamingPage<HttpHeaders, HttpHeaders>> result = pageLoader
            .load(Url.of(wireMock.url("/page.multipart")));

        result
            .test()
            .await()
            .assertError(new PageFormatException.InvalidContentType("text/plain"));
    }

    @Test
    void shouldThrowPageFormatException_whenNoBoundaryInContentType() throws InterruptedException {
        wireMock.stubFor(
            get("/page.multipart")
                .willReturn(
                    aResponse()
                        .withBodyFile("snapshot/1.content.multipart")
                        .withHeader("Content-Type", "multipart/mixed; a=b"))
        );

        final Single<StreamingPage<HttpHeaders, HttpHeaders>> result = pageLoader
            .load(Url.of(wireMock.url("/page.multipart")));

        result
            .test()
            .await()
            .assertError(new PageFormatException.NoBoundaryInContentTypeHeader("multipart/mixed; a=b"));
    }

    @Test
    void shouldThrowPageFormatException_whenInvalidMultipart() throws InterruptedException {
        wireMock.stubFor(
            get("/page.multipart")
                .willReturn(
                    aResponse()
                        .withBody("--boundary\ncontent-type:blub\n\nbody\n\n--boundary...oops, other things")
                        .withHeader("Content-Type", "multipart/mixed; boundary=boundary"))
        );

        final Single<StreamingPage<HttpHeaders, HttpHeaders>> result = pageLoader
            .load(Url.of(wireMock.url("/page.multipart")));

        result
            .toFlowable()
            .map(StreamingPage::toCompleteEntities)
            .map(FlowAdapters::toPublisher)
            .flatMap(Flowable::fromPublisher)
            .test()
            .await()
            .assertValueCount(1)
            .assertError(new PageFormatException.InvalidMultipart(new MultipartException.InvalidDelimiter(46)));
    }

    @Test
    void shouldThrowPageFormatException_whenIncompleteMultipart() throws InterruptedException {
        wireMock.stubFor(
            get("/page.multipart")
                .willReturn(
                    aResponse()
                        .withBody("--boundary\ncontent-type: text/plain\n\nbodybodybody")
                        .withHeader("Content-Type", "multipart/mixed; boundary=boundary"))
        );

        final Single<StreamingPage<HttpHeaders, HttpHeaders>> result = pageLoader
            .load(Url.of(wireMock.url("/page.multipart")));

        result
            .toFlowable()
            .map(StreamingPage::toCompleteEntities)
            .map(FlowAdapters::toPublisher)
            .flatMap(Flowable::fromPublisher)
            .test()
            .await()
            .assertNoValues()
            .assertError(new PageFormatException.InvalidMultipart(new MultipartException.UnexpectedEndOfInput(49)));
    }

    @Test
    void shouldThrowPageFormatException_whenNoContentTypeInEntityHeader() throws InterruptedException {
        wireMock.stubFor(
            get("/page.multipart")
                .willReturn(
                    aResponse()
                        .withBody("--boundary\nheader: value\n\nbody\n\n--boundary--")
                        .withHeader("Content-Type", "multipart/mixed; boundary=boundary"))
        );

        final Single<StreamingPage<HttpHeaders, HttpHeaders>> result = pageLoader
            .load(Url.of(wireMock.url("/page.multipart")));

        result
            .toFlowable()
            .map(StreamingPage::toCompleteEntities)
            .map(FlowAdapters::toPublisher)
            .flatMap(Flowable::fromPublisher)
            .test()
            .await()
            .assertNoValues()
            .assertError(new PageFormatException.MissingContentTypeInEntity(0));
    }
}
