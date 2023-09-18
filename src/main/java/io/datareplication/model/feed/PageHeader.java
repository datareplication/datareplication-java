package io.datareplication.model.feed;

import io.datareplication.model.Header;
import io.datareplication.model.HttpHeaders;
import io.datareplication.model.ToHttpHeaders;
import lombok.Value;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Optional;

@Value
public class PageHeader implements ToHttpHeaders {
    Instant lastModified;
    Link self;
    Optional<Link> prev;
    Optional<Link> next;
    HttpHeaders extraHeaders;

    @Override
    public HttpHeaders toHttpHeaders() {
        return extraHeaders
                .update(Header.lastModified(lastModified))
                .update(linkHeader());
    }

    private Header linkHeader() {
        ArrayList<String> links = new ArrayList<>();
        links.add(formatLink(self, "self"));
        prev.ifPresent(link -> links.add(formatLink(link, "prev")));
        next.ifPresent(link -> links.add(formatLink(link, "next")));
        return Header.of(Header.LINK, links);
    }

    private String formatLink(Link link, String rel) {
        return String.format("<%s>; rel=%s", link.value().value(), rel);
    }
}
