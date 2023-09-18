package io.datareplication.model.feed;

import io.datareplication.model.Header;
import io.datareplication.model.HttpHeaders;
import io.datareplication.model.ToHttpHeaders;
import lombok.Value;

import java.time.Instant;
import java.util.Optional;

@Value
public class EntityHeader implements ToHttpHeaders {
    Instant lastModified;
    OperationType operationType;
    Optional<ContentId> contentId;
    HttpHeaders extraHeaders;

    @Override
    public HttpHeaders toHttpHeaders() {
        HttpHeaders updated = extraHeaders
                .update(Header.lastModified(lastModified))
                .update(operationTypeHeader());
        if (contentId.isPresent()) {
            updated = updated.update(Header.of(Header.CONTENT_ID, contentId.get().value()));
        }
        return updated;
    }

    private Header operationTypeHeader() {
        return Header.of(Header.OPERATION_TYPE, String.format("http-equiv=%s", operationType));
    }
}
