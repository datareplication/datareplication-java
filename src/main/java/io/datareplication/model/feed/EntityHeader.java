package io.datareplication.model.feed;

import io.datareplication.model.HttpHeader;
import io.datareplication.model.HttpHeaders;
import io.datareplication.model.Timestamp;
import io.datareplication.model.ToHttpHeaders;
import lombok.Value;

import java.util.Optional;

@Value
public class EntityHeader implements ToHttpHeaders {
    Timestamp lastModified;
    OperationType operationType;
    Optional<ContentId> contentId;
    HttpHeaders extraHeaders;

    @Override
    public HttpHeaders toHttpHeaders() {
        HttpHeaders updated = extraHeaders
                .update(HttpHeader.lastModified(lastModified))
                .update(operationTypeHeader());
        if (contentId.isPresent()) {
            updated = updated.update(HttpHeader.of(HttpHeader.CONTENT_ID, contentId.get().value()));
        }
        return updated;
    }

    private HttpHeader operationTypeHeader() {
        return HttpHeader.of(HttpHeader.OPERATION_TYPE, String.format("http-equiv=%s", operationType));
    }
}
