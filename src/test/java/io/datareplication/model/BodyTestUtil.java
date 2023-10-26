package io.datareplication.model;

import lombok.experimental.UtilityClass;
import org.apache.commons.io.IOUtils;
import org.assertj.core.api.recursive.comparison.RecursiveComparisonConfiguration;

import java.io.IOException;

@UtilityClass
public final class BodyTestUtil {
    public static boolean bodiesEqual(final Body a, final Body b) {
        try (var in1 = a.newInputStream()) {
            try (var in2 = b.newInputStream()) {
                return IOUtils.contentEquals(in1, in2)
                    && a.contentLength() == b.contentLength()
                    && a.contentType().equals(b.contentType());
            }
        } catch (IOException e) {
            throw new IllegalStateException("IOException when comparing Body", e);
        }
    }

    public static RecursiveComparisonConfiguration bodyContentsComparator() {
        return RecursiveComparisonConfiguration
            .builder()
            .withEqualsForType(BodyTestUtil::bodiesEqual, Body.class)
            .build();
    }
}
