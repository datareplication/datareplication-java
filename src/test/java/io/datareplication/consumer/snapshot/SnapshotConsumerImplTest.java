package io.datareplication.consumer.snapshot;

import io.datareplication.consumer.HttpException;
import io.datareplication.internal.http.HttpClient;
import io.datareplication.internal.http.TestHttpResponse;
import io.datareplication.internal.page.PageLoader;
import io.datareplication.model.Timestamp;
import io.datareplication.model.Url;
import io.datareplication.model.snapshot.SnapshotId;
import io.datareplication.model.snapshot.SnapshotIndex;
import io.reactivex.rxjava3.core.Single;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SnapshotConsumerImplTest {
    @Mock
    private HttpClient httpClient;
    @Mock
    private PageLoader pageLoader;
    @InjectMocks
    private SnapshotConsumerImpl snapshotConsumer;

    private static final Url SOME_URL = Url.of("https://example.datareplication.io/snapshotindex.json");

    private static String resourceAsString(String name) {
        try (InputStream stream = SnapshotConsumerImpl.class.getClassLoader().getResourceAsStream(name)) {
            return IOUtils.toString(Objects.requireNonNull(stream), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void loadSnapshotIndex_shouldLoadSnapshotIndex() throws InterruptedException {
        final String snapshotIndexJson = resourceAsString("__files/snapshot/index.json");
        final SnapshotIndex expectedSnapshotIndex = new SnapshotIndex(
            SnapshotId.of("example"),
            Timestamp.of(Instant.parse("2023-10-07T15:00:00.000Z")),
            List.of(
                Url.of("https://localhost:8443/1.content.multipart"),
                Url.of("https://localhost:8443/2.content.multipart"),
                Url.of("https://localhost:8443/3.content.multipart")
            )
        );
        when(httpClient.get(eq(SOME_URL), any())).thenReturn(
            Single.just(new TestHttpResponse<>(snapshotIndexJson.getBytes(StandardCharsets.UTF_8)))
        );

        Single
            .fromCompletionStage(snapshotConsumer.loadSnapshotIndex(SOME_URL))
            .test()
            .await()
            .assertValue(expectedSnapshotIndex);
    }

    @Test
    void loadSnapshotIndex_shouldThrowParsingException_whenInvalidJson() throws InterruptedException {
        when(httpClient.get(eq(SOME_URL), any())).thenReturn(
            Single.just(new TestHttpResponse<>("{\"key\":4".getBytes(StandardCharsets.UTF_8)))
        );

        Single
            .fromCompletionStage(snapshotConsumer.loadSnapshotIndex(SOME_URL))
            .test()
            .await()
            .assertNoValues()
            .assertError(SnapshotIndex.ParsingException.class);
    }

    @Test
    void loadSnapshotIndex_shouldThrowHttpException_fromUnderlyingHttpClient() throws InterruptedException {
        when(httpClient.get(eq(SOME_URL), any())).thenReturn(
            Single.error(new HttpException.ClientError(404))
        );

        Single
            .fromCompletionStage(snapshotConsumer.loadSnapshotIndex(SOME_URL))
            .test()
            .await()
            .assertNoValues()
            .assertError(new HttpException.ClientError(404));
    }
}
