package io.datareplication.model.feed;

import io.datareplication.model.HttpHeader;
import io.datareplication.model.HttpHeaders;
import io.datareplication.model.Timestamp;
import io.datareplication.model.ToHttpHeaders;
import lombok.Value;

import java.util.ArrayList;
import java.util.Optional;

@Value
public class PageHeader implements ToHttpHeaders {
    Timestamp lastModified;
    Link self;
    Optional<Link> prev;
    Optional<Link> next;
    HttpHeaders extraHeaders;

    @Override
    public HttpHeaders toHttpHeaders() {
        return extraHeaders
                .update(HttpHeader.lastModified(lastModified))
                .update(linkHeader());
    }

    private HttpHeader linkHeader() {
        ArrayList<String> links = new ArrayList<>();
        links.add(formatLink(self, "self"));
        prev.ifPresent(link -> links.add(formatLink(link, "prev")));
        next.ifPresent(link -> links.add(formatLink(link, "next")));
        return HttpHeader.of(HttpHeader.LINK, links);
    }

    private String formatLink(Link link, String rel) {
        return String.format("<%s>; rel=%s", link.value().value(), rel);
    }
}
