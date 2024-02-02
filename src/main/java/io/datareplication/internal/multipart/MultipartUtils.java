package io.datareplication.internal.multipart;

import io.datareplication.model.ContentType;
import io.datareplication.model.PageId;

public class MultipartUtils {
    private MultipartUtils() {}

    // TODO?: maybe this should validate that this is a valid boundary; alternatively we should validate that when
    //  creating page IDs
    //  From RFC 2046
    //     boundary := 0*69<bchars> bcharsnospace
    //     bchars := bcharsnospace / " "
    //     bcharsnospace := DIGIT / ALPHA / "'" / "(" / ")" /
    //                      "+" / "_" / "," / "-" / "." /
    //                      "/" / ":" / "=" / "?"
    public static String defaultBoundary(PageId pageId) {
        return String.format("_---_%s", pageId.value());
    }

    public static ContentType pageContentType(String boundary) {
        return ContentType.of(String.format("multipart/mixed; boundary=\"%s\"", boundary));
    }
}
