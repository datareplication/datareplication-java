package io.datareplication.internal.http;

import io.datareplication.consumer.PageFormatException;
import lombok.Value;

@Value
class MultipartContentType {
    String mediaType;
    String boundary;

    static MultipartContentType parse(String contentType) {
        HeaderFieldValue parsed;
        try {
            parsed = HeaderFieldValue.parse(contentType);
        } catch (IllegalArgumentException exc) {
            throw new PageFormatException.UnparseableContentTypeHeader(contentType, exc);
        }
        if (!getMediaTypeCategory(parsed.mainValue()).equals("multipart")) {
            throw new PageFormatException.InvalidContentType(parsed.mainValue());
        }
        final String boundary = parsed
            .parameter("boundary")
            .orElseThrow(() -> new PageFormatException.NoBoundaryInContentTypeHeader(contentType));
        return new MultipartContentType(parsed.mainValue(), boundary);
    }

    private static String getMediaTypeCategory(String mediaType) {
        final int idx = mediaType.indexOf('/');
        if (idx == -1) {
            return mediaType.toLowerCase();
        } else {
            return mediaType.substring(0, idx).toLowerCase();
        }
    }
}
