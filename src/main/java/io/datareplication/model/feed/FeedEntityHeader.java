package io.datareplication.model.feed;

import io.datareplication.model.HttpHeader;
import io.datareplication.model.HttpHeaders;
import io.datareplication.model.Timestamp;
import io.datareplication.model.ToHttpHeaders;
import lombok.NonNull;
import lombok.Value;

@Value
public class FeedEntityHeader implements ToHttpHeaders {
    @NonNull Timestamp lastModified;
    @NonNull OperationType operationType;
    @NonNull ContentId contentId;
    @NonNull HttpHeaders extraHeaders;

    @Override
    public @NonNull HttpHeaders toHttpHeaders() {
        return extraHeaders
                .update(HttpHeader.lastModified(lastModified))
                .update(HttpHeader.of(HttpHeader.CONTENT_ID, contentId.value()))
                .update(operationTypeHeader());
    }

    private HttpHeader operationTypeHeader() {
        return HttpHeader.of(HttpHeader.OPERATION_TYPE, String.format("http-equiv=%s", operationType));
    }
}
