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
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.reactivestreams.FlowAdapters;

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

        final StreamingPage<HttpHeaders, HttpHeaders> page = pageLoader
            .load(Url.of(WM.url("/page.multipart")))
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
        WM.stubFor(
            get("/page.multipart").willReturn(
                aResponse()
                    .withBodyFile("snapshot/1.content.multipart")
                    .withHeader("Content-Type-oops-no", "multipart/mixed; boundary=<random-boundary>")
            ));

        final Single<StreamingPage<HttpHeaders, HttpHeaders>> result = pageLoader
            .load(Url.of(WM.url("/page.multipart")));

        result
            .test()
            .await()
            .assertError(new PageFormatException.MissingContentTypeHeader());
    }

    @Test
    void shouldThrowPageFormatException_whenNotMultipart() throws InterruptedException {
        WM.stubFor(
            get("/page.multipart").willReturn(
                aResponse()
                    .withBodyFile("snapshot/1.content.multipart")
                    .withHeader("Content-Type", "text/plain; boundary=<random-boundary>")
            ));

        final Single<StreamingPage<HttpHeaders, HttpHeaders>> result = pageLoader
            .load(Url.of(WM.url("/page.multipart")));

        result
            .test()
            .await()
            .assertError(new PageFormatException.InvalidContentType("text/plain"));
    }

    @Test
    void shouldThrowPageFormatException_whenNoBoundaryInContentType() throws InterruptedException {
        WM.stubFor(
            get("/page.multipart").willReturn(
                aResponse()
                    .withBodyFile("snapshot/1.content.multipart")
                    .withHeader("Content-Type", "multipart/mixed; a=b")
            ));

        final Single<StreamingPage<HttpHeaders, HttpHeaders>> result = pageLoader
            .load(Url.of(WM.url("/page.multipart")));

        result
            .test()
            .await()
            .assertError(new PageFormatException.NoBoundaryInContentTypeHeader("multipart/mixed; a=b"));
    }

    @Test
    void shouldThrowHttpException_whenReadTimeoutInBody() throws InterruptedException {
        final HttpClient httpClient = new HttpClient(
            AuthSupplier.none(),
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

        final Single<StreamingPage<HttpHeaders, HttpHeaders>> result = pageLoader
            .load(url);

        result
            .toFlowable()
            .map(StreamingPage::toCompleteEntities)
            .map(FlowAdapters::toPublisher)
            .flatMap(Flowable::fromPublisher)
            .test()
            .await()
            .assertError(new HttpException.NetworkError(url, ANY_EXCEPTION));
    }

    @Test
    void shouldThrowPageFormatException_whenInvalidMultipart() throws InterruptedException {
        WM.stubFor(
            get("/page.multipart").willReturn(
                aResponse()
                    .withBody("--boundary\ncontent-type:blub\n\nbody\n\n--boundary...oops, other things")
                    .withHeader("Content-Type", "multipart/mixed; boundary=boundary")
            ));

        final Single<StreamingPage<HttpHeaders, HttpHeaders>> result = pageLoader
            .load(Url.of(WM.url("/page.multipart")));

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
        WM.stubFor(
            get("/page.multipart").willReturn(
                aResponse()
                    .withBody("--boundary\ncontent-type: text/plain\n\nbodybodybody")
                    .withHeader("Content-Type", "multipart/mixed; boundary=boundary")
            ));

        final Single<StreamingPage<HttpHeaders, HttpHeaders>> result = pageLoader
            .load(Url.of(WM.url("/page.multipart")));

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
        WM.stubFor(
            get("/page.multipart").willReturn(
                aResponse()
                    .withBody("--boundary\nheader: value\n\nbody\n\n--boundary--")
                    .withHeader("Content-Type", "multipart/mixed; boundary=boundary")
            ));

        final Single<StreamingPage<HttpHeaders, HttpHeaders>> result = pageLoader
            .load(Url.of(WM.url("/page.multipart")));

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

    @Test
    void shouldThrowPageFormatException_whenInvalidMultipartInChunkStream() throws InterruptedException {
        WM.stubFor(
            get("/page.multipart").willReturn(
                aResponse()
                    .withBody("--boundary\ncontent-type:blub\n\nbody\n--boundary...oops, other things")
                    .withHeader("Content-Type", "multipart/mixed; boundary=boundary")
            ));

        final StreamingPage<HttpHeaders, HttpHeaders> page = pageLoader
            .load(Url.of(WM.url("/page.multipart")))
            .blockingGet();

        Flowable
            .fromPublisher(FlowAdapters.toPublisher(page))
            .test()
            .await()
            .assertValues(
                StreamingPage.Chunk.header(HttpHeaders.EMPTY, ContentType.of("blub")),
                StreamingPage.Chunk.bodyChunk(ByteBuffer.wrap("body".getBytes(StandardCharsets.UTF_8))),
                StreamingPage.Chunk.bodyEnd()
            )
            .assertError(new PageFormatException.InvalidMultipart(new MultipartException.InvalidDelimiter(45)));
    }

    @Test
    void shouldThrowPageFormatException_whenInvalidMultipartInCompletePage() throws InterruptedException {
        WM.stubFor(
            get("/page.multipart").willReturn(
                aResponse()
                    .withBody("--boundary\ncontent-type:blub\n\nbody\n--boundary...oops, other things")
                    .withHeader("Content-Type", "multipart/mixed; boundary=boundary")
            ));

        final StreamingPage<HttpHeaders, HttpHeaders> page = pageLoader
            .load(Url.of(WM.url("/page.multipart")))
            .blockingGet();

        Single
            .fromCompletionStage(page.toCompletePage())
            .test()
            .await()
            .assertError(new PageFormatException.InvalidMultipart(new MultipartException.InvalidDelimiter(45)));
    }
}
