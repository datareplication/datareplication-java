package io.datareplication.internal.multipart;

import io.datareplication.model.ContentType;
import io.datareplication.model.PageId;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MultipartUtilsTest {
    @Test
    void defaultBoundary_shouldReturnBoundary() {
        var result = MultipartUtils.defaultBoundary(PageId.of("page-id"));

        assertThat(result).isEqualTo("_---_page-id");
    }

    @Test
    void pageContentType_shouldReturnContentTypeWithBoundary() {
        var result = MultipartUtils.pageContentType("-boundary_string-");

        assertThat(result).isEqualTo(ContentType.of("multipart/mixed; boundary=\"-boundary_string-\""));
    }
}
