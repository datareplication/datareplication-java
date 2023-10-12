package io.datareplication.model.snapshot;

import io.datareplication.model.Timestamp;
import io.datareplication.model.Url;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SnapshotIndexTest {
    private static final Url URL_1 = Url.of("https://example.datareplication.io/1");
    private static final Url URL_2 = Url.of("https://example.datareplication.io/2");
    private static final Url URL_3 = Url.of("https://example.datareplication.io/3");

    @Test
    void shouldMakePageListUnmodifiable() {
        final ArrayList<Url> original = new ArrayList<>();
        original.add(URL_1);
        original.add(URL_2);
        final ArrayList<Url> pages = new ArrayList<>(original);

        final SnapshotIndex index = new SnapshotIndex(SnapshotId.of("1234"), Timestamp.now(), pages);

        assertThat(index.pages()).containsExactlyElementsOf(original);
        assertThatThrownBy(() -> index.pages().add(URL_3))
            .isInstanceOf(UnsupportedOperationException.class);
        pages.add(URL_3);
        assertThat(index.pages()).containsExactlyElementsOf(original);
    }
}
