package io.datareplication.producer.snapshot;

import io.datareplication.model.PageId;
import io.datareplication.model.Timestamp;
import io.datareplication.model.Url;
import io.datareplication.model.snapshot.SnapshotId;
import io.datareplication.model.snapshot.SnapshotIndex;
import lombok.NonNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SnapshotProducerIntegrationTest {
    @Test
    @DisplayName("Should produce snapshot")
    void shouldProduceSnapshot(@Mock SnapshotPageRepository snapshotPageRepository,
                               @Mock SnapshotIndexRepository snapshotIndexRepository,
                               @Mock SnapshotPageUrlBuilder snapshotPageUrlBuilder,
                               @Mock PageIdProvider pageIdProvider,
                               @Mock SnapshotIdProvider snapshotIdProvider) {
        Timestamp now = Timestamp.of(Instant.now());
        SnapshotId snapshotId = SnapshotId.of("snapshotIdForTest");
        PageId pageId1 = PageId.of("1");
        PageId pageId2 = PageId.of("2");
        PageId pageId3 = PageId.of("3");
        List<Url> pages = List.of(
            url(snapshotId, pageId1),
            url(snapshotId, pageId2),
            url(snapshotId, pageId3)
        );

        when(pageIdProvider.newPageId()).thenReturn(pageId1).thenReturn(pageId2).thenReturn(pageId3);
        when(snapshotIdProvider.newSnapshotId()).thenReturn(snapshotId);
        when(snapshotPageUrlBuilder.pageUrl(any(SnapshotId.class), any(PageId.class))).thenAnswer(input ->
            url(input.getArgument(0), input.getArgument(1)));

        SnapshotIndex expectedSnapshotIndex = new SnapshotIndex(snapshotId, now, pages);

        SnapshotProducer snapshotProducer = SnapshotProducer
            .builder()
            // TODO: Additional configuration
            .build();

        // TODO: Create Snapshot with Snapshot Producer
        // TODO Use Entities instead of null
        snapshotProducer.produce(null);

        verify(snapshotIndexRepository).save(expectedSnapshotIndex);
        // TODO: Assert PageRepository
    }

    private @NonNull Url url(final SnapshotId snapshotId, final PageId pageId1) {
        return Url.of("https://localhost:8080/" + snapshotId.value() + "/" + pageId1.value());
    }
}
