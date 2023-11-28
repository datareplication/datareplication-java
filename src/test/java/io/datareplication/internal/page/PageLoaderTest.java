package io.datareplication.internal.page;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import io.datareplication.consumer.HttpException;
import io.datareplication.consumer.PageFormatException;
import io.datareplication.consumer.StreamingPage;
import io.datareplication.internal.http.AuthSupplier;
import io.datareplication.internal.http.HttpClient;
import io.datareplication.internal.multipart.MultipartException;
import io.datareplication.model.ContentType;
import io.datareplication.model.HttpHeader;
import io.datareplication.model.HttpHeaders;
import io.datareplication.model.Url;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import reactor.adapter.JdkFlowAdapter;
import reactor.test.StepVerifier;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static org.assertj.core.api.Assertions.assertThat;

class PageLoaderTest {
    private static final Throwable ANY_EXCEPTION = new RuntimeException();

    @RegisterExtension
    static final WireMockExtension WM = WireMockExtension
        .newInstance()
        .build();

    private final HttpClient httpClient = new HttpClient();
    private final PageLoader pageLoader = new PageLoader(httpClient);

    @Test
    void shouldDownloadAndParseMultipartPage() {
        WM.stubFor(
            get("/page.multipart").willReturn(
                aResponse()
                    .withBodyFile("snapshot/1.content.multipart")
                    .withHeader("Content-Type", "multipart/mixed; boundary=<random-boundary>")
                    .withHeader("Test-Header", "value1", "value2")
            ));

        final var page = pageLoader
            .load(Url.of(WM.url("/page.multipart")))
            .single()
            .block();

        assertThat(page.header()).contains(
            HttpHeader.of("content-type", "multipart/mixed; boundary=<random-boundary>"),
            HttpHeader.of("test-header", List.of("value1", "value2"))
        );

        final var chunks = JdkFlowAdapter
            .flowPublisherToFlux(page)
            .collectList()
            .single()
            .block();
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
    void shouldThrowPageFormatException_whenNoContentTypeHeader() {
        WM.stubFor(
            get("/page.multipart").willReturn(
                aResponse()
                    .withBodyFile("snapshot/1.content.multipart")
                    .withHeader("Content-Type-oops-no", "multipart/mixed; boundary=<random-boundary>")
            ));

        final var result = pageLoader
            .load(Url.of(WM.url("/page.multipart")));

        StepVerifier
            .create(result)
            .expectError(PageFormatException.MissingContentTypeHeader.class)
            .verify();
    }

    @Test
    void shouldThrowPageFormatException_whenNotMultipart() {
        WM.stubFor(
            get("/page.multipart").willReturn(
                aResponse()
                    .withBodyFile("snapshot/1.content.multipart")
                    .withHeader("Content-Type", "text/plain; boundary=<random-boundary>")
            ));

        final var result = pageLoader
            .load(Url.of(WM.url("/page.multipart")));

        StepVerifier
            .create(result)
            .expectErrorMatches(new PageFormatException.InvalidContentType("text/plain")::equals)
            .verify();
    }

    @Test
    void shouldThrowPageFormatException_whenNoBoundaryInContentType() {
        WM.stubFor(
            get("/page.multipart").willReturn(
                aResponse()
                    .withBodyFile("snapshot/1.content.multipart")
                    .withHeader("Content-Type", "multipart/mixed; a=b")
            ));

        final var result = pageLoader
            .load(Url.of(WM.url("/page.multipart")));

        StepVerifier
            .create(result)
            .expectErrorMatches(new PageFormatException.NoBoundaryInContentTypeHeader("multipart/mixed; a=b")::equals)
            .verify();
    }

    @Test
    void shouldThrowHttpException_whenReadTimeoutInBody() {
        final HttpClient httpClient = new HttpClient(
            AuthSupplier.none(),
            HttpHeaders.EMPTY,
            Optional.empty(),
            Optional.of(Duration.ofMillis(10))
        );
        final PageLoader pageLoader = new PageLoader(httpClient);

        WM.stubFor(
            get("/page.multipart").willReturn(
                aResponse()
                    .withBodyFile("snapshot/1.content.multipart")
                    .withChunkedDribbleDelay(5, 500)
                    .withHeader("Content-Type", "multipart/mixed; boundary=<random-boundary>")
            ));
        final var url = Url.of(WM.url("/page.multipart"));

        final var result = pageLoader
            .load(url)
            .single()
            .block();

        StepVerifier
            .create(JdkFlowAdapter.flowPublisherToFlux(result.toCompleteEntities()))
            .expectErrorMatches(new HttpException.NetworkError(url, ANY_EXCEPTION)::equals)
            .verify();
    }

    @Test
    void shouldThrowPageFormatException_whenInvalidMultipart() {
        WM.stubFor(
            get("/page.multipart").willReturn(
                aResponse()
                    .withBody("--boundary\ncontent-type:blub\n\nbody\n\n--boundary...oops, other things")
                    .withHeader("Content-Type", "multipart/mixed; boundary=boundary")
            ));

        final var result = pageLoader
            .load(Url.of(WM.url("/page.multipart")))
            .single()
            .block();

        StepVerifier
            .create(JdkFlowAdapter.flowPublisherToFlux(result.toCompleteEntities()))
            .expectNextCount(1)
            .expectErrorMatches(
                new PageFormatException.InvalidMultipart(new MultipartException.InvalidDelimiter(46))::equals
            )
            .verify();
    }

    @Test
    void shouldThrowPageFormatException_whenIncompleteMultipart() {
        WM.stubFor(
            get("/page.multipart").willReturn(
                aResponse()
                    .withBody("--boundary\ncontent-type: text/plain\n\nbodybodybody")
                    .withHeader("Content-Type", "multipart/mixed; boundary=boundary")
            ));

        final var result = pageLoader
            .load(Url.of(WM.url("/page.multipart")))
            .single()
            .block();

        StepVerifier
            .create(JdkFlowAdapter.flowPublisherToFlux(result.toCompleteEntities()))
            .expectNextCount(0)
            .expectErrorMatches(
                new PageFormatException.InvalidMultipart(new MultipartException.UnexpectedEndOfInput(49))::equals
            )
            .verify();
    }

    @Test
    void shouldThrowPageFormatException_whenNoContentTypeInEntityHeader() {
        WM.stubFor(
            get("/page.multipart").willReturn(
                aResponse()
                    .withBody("--boundary\nheader: value\n\nbody\n\n--boundary--")
                    .withHeader("Content-Type", "multipart/mixed; boundary=boundary")
            ));

        final var result = pageLoader
            .load(Url.of(WM.url("/page.multipart")))
            .single()
            .block();

        StepVerifier
            .create(JdkFlowAdapter.flowPublisherToFlux(result.toCompleteEntities()))
            .expectNextCount(0)
            .expectErrorMatches(new PageFormatException.MissingContentTypeInEntity(0)::equals)
            .verify();
    }

    @Test
    void shouldThrowPageFormatException_whenInvalidMultipartInChunkStream() {
        WM.stubFor(
            get("/page.multipart").willReturn(
                aResponse()
                    .withBody("--boundary\ncontent-type:blub\n\nbody\n--boundary...oops, other things")
                    .withHeader("Content-Type", "multipart/mixed; boundary=boundary")
            ));

        final var page = pageLoader
            .load(Url.of(WM.url("/page.multipart")))
            .single()
            .block();

        StepVerifier
            .create(JdkFlowAdapter.flowPublisherToFlux(page))
            .expectNext(
                StreamingPage.Chunk.header(HttpHeaders.EMPTY, ContentType.of("blub")),
                StreamingPage.Chunk.bodyChunk(ByteBuffer.wrap("body".getBytes(StandardCharsets.UTF_8))),
                StreamingPage.Chunk.bodyEnd()
            )
            .expectErrorMatches(
                new PageFormatException.InvalidMultipart(new MultipartException.InvalidDelimiter(45))::equals
            )
            .verify();
    }

    @Test
    void shouldThrowPageFormatException_whenInvalidMultipartInCompletePage() {
        WM.stubFor(
            get("/page.multipart").willReturn(
                aResponse()
                    .withBody("--boundary\ncontent-type:blub\n\nbody\n--boundary...oops, other things")
                    .withHeader("Content-Type", "multipart/mixed; boundary=boundary")
            ));

        final var page = pageLoader
            .load(Url.of(WM.url("/page.multipart")))
            .single()
            .block();

        StepVerifier
            .create(JdkFlowAdapter.flowPublisherToFlux(page))
            .expectNextCount(3)
            .expectErrorMatches(
                new PageFormatException.InvalidMultipart(new MultipartException.InvalidDelimiter(45))::equals
            )
            .verify();
    }
}
