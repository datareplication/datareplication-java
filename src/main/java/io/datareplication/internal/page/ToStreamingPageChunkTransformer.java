package io.datareplication.internal.page;

import io.datareplication.consumer.PageFormatException;
import io.datareplication.consumer.StreamingPage;
import io.datareplication.internal.multipart.Token;
import io.datareplication.model.ContentType;
import io.datareplication.model.HttpHeader;
import io.datareplication.model.HttpHeaders;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Transform a stream of multipart {@link Token Tokens} into higher-level {@link StreamingPage.Chunk} objects. This
 * mostly involves collecting headers into {@link HttpHeaders} and returning them as a block.
 */
final class ToStreamingPageChunkTransformer {
    private final List<HttpHeader> headers = new ArrayList<>();
    private Optional<ContentType> contentType = Optional.empty();
    private int index;

    /**
     * Consume the given {@link Token} and optionally return a {@link StreamingPage.Chunk}.
     *
     * @param multipartToken a token from the multipart parser
     * @throws PageFormatException.MissingContentTypeInEntity if an entity doesn't have a content-type header
     * @return a Chunk if this token needs one to be emitted
     */
    public Optional<StreamingPage.Chunk<HttpHeaders>> transform(Token multipartToken) {
        if (multipartToken instanceof Token.Continue) {
            return Optional.empty();
        } else if (multipartToken instanceof Token.PartBegin) {
            headers.clear();
            contentType = Optional.empty();
            return Optional.empty();
        } else if (multipartToken instanceof Token.Header) {
            final Token.Header header = (Token.Header) multipartToken;
            final HttpHeader httpHeader = HttpHeader.of(header.name(), header.value());
            if (httpHeader.nameEquals(HttpHeader.CONTENT_TYPE)) {
                contentType = Optional.of(ContentType.of(header.value()));
            } else {
                headers.add(httpHeader);
            }
            return Optional.empty();
        } else if (multipartToken instanceof Token.DataBegin) {
            return Optional.of(StreamingPage.Chunk.header(
                    HttpHeaders.of(headers),
                    contentType.orElseThrow(() -> new PageFormatException.MissingContentTypeInEntity(index))
            ));
        } else if (multipartToken instanceof Token.Data) {
            final Token.Data data = (Token.Data) multipartToken;
            return Optional.of(StreamingPage.Chunk.bodyChunk(data.data()));
        } else if (multipartToken instanceof Token.PartEnd) {
            index++;
            return Optional.of(StreamingPage.Chunk.bodyEnd());
        }
        throw new IllegalArgumentException(String.format("unknown subclass of Token %s; bug?", multipartToken));
    }
}
