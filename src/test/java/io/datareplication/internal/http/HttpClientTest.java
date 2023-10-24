package io.datareplication.internal.http;

import com.github.mizosoft.methanol.Methanol;
import com.github.tomakehurst.wiremock.http.Fault;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import io.datareplication.consumer.HttpException;
import io.datareplication.model.Url;
import io.reactivex.rxjava3.core.Single;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static org.assertj.core.api.Assertions.assertThat;

class HttpClientTest {
    private static final Throwable ANY_EXCEPTION = new RuntimeException();

    @RegisterExtension
    final WireMockExtension wireMock = WireMockExtension
        .newInstance()
        .build();

    private final HttpClient httpClient = new HttpClient(Methanol.newBuilder().build());

    @Test
    void shouldGet() {
        wireMock.stubFor(
            get("/").willReturn(
                aResponse()
                    .withStatus(200)
                    .withBody("body content")
                    .withHeader("Content-Type", "text/plain")
                    .withHeader("Test-Header", "value1", "value2"))
        );

        final HttpResponse<String> response = httpClient
            .get(Url.of(wireMock.url("/")), HttpResponse.BodyHandlers.ofString())
            .blockingGet();

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().map())
            .containsAllEntriesOf(Map.of(
                "Content-Type", List.of("text/plain"),
                "Test-Header", List.of("value1", "value2")
            ));
        assertThat(response.body()).isEqualTo("body content");
    }

    @Test
    void shouldThrowHttpException_whenInvalidUrl() throws InterruptedException {
        final Url url = Url.of("I'm an invalid URL");

        final Single<HttpResponse<Void>> result = httpClient
            .get(url, HttpResponse.BodyHandlers.discarding());

        result
            .test()
            .await()
            .assertError(new HttpException.InvalidUrl(url, ANY_EXCEPTION));
    }

    @Test
    void shouldThrowHttpException_whenInvalidScheme() throws InterruptedException {
        final Url url = Url.of("ftp://example.com/a/b/c");

        final Single<HttpResponse<Void>> result = httpClient
            .get(url, HttpResponse.BodyHandlers.discarding());

        result
            .test()
            .await()
            .assertError(new HttpException.InvalidUrl(url, ANY_EXCEPTION));
    }

    @Test
    void shouldThrowHttpException_whenHttp404() throws InterruptedException {
        wireMock.stubFor(
            get("/").willReturn(
                aResponse()
                    .withStatus(404)
                    .withBody("this is not the url you're looking for")
            ));
        final var url = Url.of(wireMock.url("/"));

        final Single<HttpResponse<Void>> result = httpClient
            .get(url, HttpResponse.BodyHandlers.discarding());

        result
            .test()
            .await()
            .assertError(new HttpException.ClientError(url, 404));
    }

    @Test
    void shouldThrowHttpException_whenHttp500() throws InterruptedException {
        wireMock.stubFor(
            get("/").willReturn(
                aResponse()
                    .withStatus(500)
                    .withBody("oops")
            ));
        final var url = Url.of(wireMock.url("/"));

        final Single<HttpResponse<Void>> result = httpClient
            .get(url, HttpResponse.BodyHandlers.discarding());

        result
            .test()
            .await()
            .assertError(new HttpException.ServerError(url, 500));
    }

    @Test
    void shouldThrowHttpException_whenEmptyResponse() throws InterruptedException {
        wireMock.stubFor(
            get("/").willReturn(
                aResponse().withFault(Fault.EMPTY_RESPONSE)
            ));
        final var url = Url.of(wireMock.url("/"));

        final Single<HttpResponse<Void>> result = httpClient
            .get(url, HttpResponse.BodyHandlers.discarding());

        result
            .test()
            .await()
            .assertError(new HttpException.NetworkError(url, ANY_EXCEPTION));
    }

    @Test
    void shouldThrowHttpException_whenMalformedResponseChunk() throws InterruptedException {
        wireMock.stubFor(
            get("/").willReturn(
                aResponse().withFault(Fault.MALFORMED_RESPONSE_CHUNK)
            ));
        final var url = Url.of(wireMock.url("/"));

        final Single<HttpResponse<Void>> result = httpClient
            .get(url, HttpResponse.BodyHandlers.discarding());

        result
            .test()
            .await()
            .assertError(new HttpException.NetworkError(url, ANY_EXCEPTION));
    }

    @Test
    void shouldThrowHttpException_whenRandomDataThenClose() throws InterruptedException {
        wireMock.stubFor(
            get("/").willReturn(
                aResponse().withFault(Fault.RANDOM_DATA_THEN_CLOSE)
            ));
        final var url = Url.of(wireMock.url("/"));

        final Single<HttpResponse<Void>> result = httpClient
            .get(url, HttpResponse.BodyHandlers.discarding());

        result
            .test()
            .await()
            .assertError(new HttpException.NetworkError(url, ANY_EXCEPTION));
    }

    @Test
    void shouldThrowHttpException_whenConnectionResetByPeer() throws InterruptedException {
        wireMock.stubFor(
            get("/").willReturn(
                aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)
            ));
        final var url = Url.of(wireMock.url("/"));

        final Single<HttpResponse<Void>> result = httpClient
            .get(url, HttpResponse.BodyHandlers.discarding());

        result
            .test()
            .await()
            .assertError(new HttpException.NetworkError(url, ANY_EXCEPTION));
    }

    @Test
    void shouldThrowHttpException_whenHeaderTimeout() throws InterruptedException {
        final HttpClient httpClient = new HttpClient(
            Methanol.newBuilder().headersTimeout(Duration.ofMillis(5)).build());
        wireMock.stubFor(
            get("/").willReturn(
                aResponse().withFixedDelay(500)
            ));
        final var url = Url.of(wireMock.url("/"));

        final Single<HttpResponse<Void>> result = httpClient
            .get(url, HttpResponse.BodyHandlers.discarding());

        result
            .test()
            .await()
            .assertError(new HttpException.NetworkError(url, ANY_EXCEPTION));
    }

    @Test
    void shouldThrowHttpException_whenReadTimeout() throws InterruptedException {
        final HttpClient httpClient = new HttpClient(
            Methanol.newBuilder().readTimeout(Duration.ofMillis(10)).build());
        wireMock.stubFor(
            get("/").willReturn(
                aResponse()
                    .withBody("12345")
                    .withChunkedDribbleDelay(5, 500)
            ));
        final var url = Url.of(wireMock.url("/"));

        final Single<HttpResponse<Void>> result = httpClient
            .get(url, HttpResponse.BodyHandlers.discarding());

        result
            .test()
            .await()
            .assertError(new HttpException.NetworkError(url, ANY_EXCEPTION));
    }
}
