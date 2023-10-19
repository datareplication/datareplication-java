package io.datareplication.internal.page;

import io.datareplication.consumer.StreamingPage;
import io.datareplication.internal.multipart.Token;
import io.datareplication.model.ContentType;
import io.datareplication.model.HttpHeader;
import io.datareplication.model.HttpHeaders;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

final class ToStreamingPageChunkTransformer {
    private final List<HttpHeader> headers = new ArrayList<>();
    private ContentType contentType = null;
    private long contentLength = 0;

    public Optional<StreamingPage.Chunk<HttpHeaders>> transform(Token multipartElem) {
        if (multipartElem instanceof Token.Continue) {
            return Optional.empty();
        } else if (multipartElem instanceof Token.PartBegin) {
            headers.clear();
            contentType = null;
            contentLength = 0;
            return Optional.empty();
        } else if (multipartElem instanceof Token.Header) {
            final Token.Header header = (Token.Header) multipartElem;
            if (header.name().equalsIgnoreCase(HttpHeader.CONTENT_TYPE)) {
                contentType = ContentType.of(header.value());
            } else if (header.name().equalsIgnoreCase(HttpHeader.CONTENT_LENGTH)) {
                // TODO: error handling
                contentLength = Long.parseLong(header.value());
            } else {
                headers.add(HttpHeader.of(header.name(), header.value()));
            }
            return Optional.empty();
        } else if (multipartElem instanceof Token.DataBegin) {
            // TODO: error handling
            if (contentLength == 0) {
                throw new RuntimeException("no contentLength");
            }
            if (contentType == null) {
                throw new RuntimeException("no contentType");
            }
            return Optional.of(StreamingPage.Chunk.header(
                HttpHeaders.of(headers),
                contentLength,
                contentType
            ));
        } else if (multipartElem instanceof Token.Data) {
            final Token.Data data = (Token.Data) multipartElem;
            return Optional.of(StreamingPage.Chunk.bodyChunk(data.data()));
        } else if (multipartElem instanceof Token.PartEnd) {
            return Optional.of(StreamingPage.Chunk.bodyEnd());
        }
        throw new IllegalArgumentException(String.format("unknown subclass of Elem %s; bug?", multipartElem));
    }
}
