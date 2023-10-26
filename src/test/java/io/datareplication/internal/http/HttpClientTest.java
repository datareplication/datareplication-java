package io.datareplication.internal.http;

import com.github.tomakehurst.wiremock.http.Fault;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import io.datareplication.consumer.Authorization;
import io.datareplication.consumer.HttpException;
import io.datareplication.model.Url;
import io.reactivex.rxjava3.core.Single;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static org.assertj.core.api.Assertions.assertThat;

class HttpClientTest {
    private static final Throwable ANY_EXCEPTION = new RuntimeException();

    @RegisterExtension
    static final WireMockExtension WM = WireMockExtension
        .newInstance()
        .build();

    private final HttpClient httpClient = new HttpClient();

    @Test
    void shouldGet() {
        WM.stubFor(
            get("/").willReturn(
                aResponse()
                    .withStatus(200)
                    .withBody("body content")
                    .withHeader("Content-Type", "text/plain")
                    .withHeader("Test-Header", "value1", "value2"))
        );

        final HttpResponse<String> response = httpClient
            .get(Url.of(WM.url("/")), HttpResponse.BodyHandlers.ofString())
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
        WM.stubFor(
            get("/").willReturn(
                aResponse()
                    .withStatus(404)
                    .withBody("this is not the url you're looking for")
            ));
        final var url = Url.of(WM.url("/"));

        final Single<HttpResponse<Void>> result = httpClient
            .get(url, HttpResponse.BodyHandlers.discarding());

        result
            .test()
            .await()
            .assertError(new HttpException.ClientError(url, 404));
    }

    @Test
    void shouldThrowHttpException_whenHttp500() throws InterruptedException {
        WM.stubFor(
            get("/").willReturn(
                aResponse()
                    .withStatus(500)
                    .withBody("oops")
            ));
        final var url = Url.of(WM.url("/"));

        final Single<HttpResponse<Void>> result = httpClient
            .get(url, HttpResponse.BodyHandlers.discarding());

        result
            .test()
            .await()
            .assertError(new HttpException.ServerError(url, 500));
    }

    @Test
    void shouldThrowHttpException_whenEmptyResponse() throws InterruptedException {
        WM.stubFor(
            get("/").willReturn(
                aResponse().withFault(Fault.EMPTY_RESPONSE)
            ));
        final var url = Url.of(WM.url("/"));

        final Single<HttpResponse<Void>> result = httpClient
            .get(url, HttpResponse.BodyHandlers.discarding());

        result
            .test()
            .await()
            .assertError(new HttpException.NetworkError(url, ANY_EXCEPTION));
    }

    @Test
    void shouldThrowHttpException_whenMalformedResponseChunk() throws InterruptedException {
        WM.stubFor(
            get("/").willReturn(
                aResponse().withFault(Fault.MALFORMED_RESPONSE_CHUNK)
            ));
        final var url = Url.of(WM.url("/"));

        final Single<HttpResponse<Void>> result = httpClient
            .get(url, HttpResponse.BodyHandlers.discarding());

        result
            .test()
            .await()
            .assertError(new HttpException.NetworkError(url, ANY_EXCEPTION));
    }

    @Test
    void shouldThrowHttpException_whenRandomDataThenClose() throws InterruptedException {
        WM.stubFor(
            get("/").willReturn(
                aResponse().withFault(Fault.RANDOM_DATA_THEN_CLOSE)
            ));
        final var url = Url.of(WM.url("/"));

        final Single<HttpResponse<Void>> result = httpClient
            .get(url, HttpResponse.BodyHandlers.discarding());

        result
            .test()
            .await()
            .assertError(new HttpException.NetworkError(url, ANY_EXCEPTION));
    }

    @Test
    void shouldThrowHttpException_whenConnectionResetByPeer() throws InterruptedException {
        WM.stubFor(
            get("/").willReturn(
                aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)
            ));
        final var url = Url.of(WM.url("/"));

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
            AuthSupplier.none(),
            Optional.of(Duration.ofMillis(5)),
            Optional.empty()
        );
        WM.stubFor(
            get("/").willReturn(
                aResponse().withFixedDelay(500)
            ));
        final var url = Url.of(WM.url("/"));

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
            AuthSupplier.none(),
            Optional.empty(),
            Optional.of(Duration.ofMillis(5))
        );
        WM.stubFor(
            get("/").willReturn(
                aResponse()
                    .withBody("12345")
                    .withChunkedDribbleDelay(5, 500)
            ));
        final var url = Url.of(WM.url("/"));

        final Single<HttpResponse<Void>> result = httpClient
            .get(url, HttpResponse.BodyHandlers.discarding());

        result
            .test()
            .await()
            .assertError(new HttpException.NetworkError(url, ANY_EXCEPTION));
    }

    @Test
    void shouldCallAuthSupplierForEveryRequest() throws InterruptedException {
        final HttpClient httpClient = new HttpClient(
            (url) -> {
                final var idx = url.value().lastIndexOf('/');
                return Optional.of(Authorization.of("Test", url.value().substring(idx)));
            },
            Optional.empty(),
            Optional.empty()
        );

        WM.stubFor(
            get("/1").withHeader("Authorization", equalTo("Test /1")).willReturn(
                aResponse()
                    .withStatus(200)
                    .withBody("1"))
        );
        WM.stubFor(
            get("/2").withHeader("Authorization", equalTo("Test /2")).willReturn(
                aResponse()
                    .withStatus(200)
                    .withBody("2"))
        );

        httpClient
            .get(Url.of(WM.url("/1")), HttpResponse.BodyHandlers.ofString())
            .map(HttpResponse::body)
            .test()
            .await()
            .assertValue("1");

        httpClient
            .get(Url.of(WM.url("/2")), HttpResponse.BodyHandlers.ofString())
            .map(HttpResponse::body)
            .test()
            .await()
            .assertValue("2");
    }
}
