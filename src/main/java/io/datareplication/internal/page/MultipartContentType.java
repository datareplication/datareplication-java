package io.datareplication.internal.page;

import io.datareplication.consumer.PageFormatException;
import io.datareplication.internal.http.HeaderFieldValue;
import lombok.Value;

import java.util.Locale;

@Value
public class MultipartContentType {
    private static final String SUPPORTED_MEDIA_TYPE_CATEGORY = "multipart";
    private static final char MULTIPART_TYPE_DELIMITER = '/';
    private static final String BOUNDARY_KEY = "boundary";
    String mediaType;
    String boundary;

    public static MultipartContentType parse(String contentType) {
        HeaderFieldValue parsed;
        try {
            parsed = HeaderFieldValue.parse(contentType);
        } catch (IllegalArgumentException exc) {
            throw new PageFormatException.UnparseableContentTypeHeader(contentType, exc);
        }
        if (!SUPPORTED_MEDIA_TYPE_CATEGORY.equals(getMediaTypeCategory(parsed.mainValue()))) {
            throw new PageFormatException.InvalidContentType(parsed.mainValue());
        }
        final String boundary = parsed
            .parameter(BOUNDARY_KEY)
            .orElseThrow(() -> new PageFormatException.NoBoundaryInContentTypeHeader(contentType));
        return new MultipartContentType(parsed.mainValue(), boundary);
    }

    private static String getMediaTypeCategory(String mediaType) {
        final int idx = mediaType.indexOf(MULTIPART_TYPE_DELIMITER);
        if (idx == -1) {
            return mediaType.toLowerCase(Locale.ENGLISH);
        } else {
            return mediaType.substring(0, idx).toLowerCase(Locale.ENGLISH);
        }
    }
}
