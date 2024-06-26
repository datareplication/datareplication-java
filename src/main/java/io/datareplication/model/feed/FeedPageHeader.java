package io.datareplication.model.feed;

import io.datareplication.model.HttpHeader;
import io.datareplication.model.HttpHeaders;
import io.datareplication.model.ToHttpHeaders;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.Value;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Optional;

/**
 * This class represents the headers of a snapshot page.
 */
@Value
@AllArgsConstructor
public class FeedPageHeader implements ToHttpHeaders {
    /**
     * The page's timestamp. This is equal to the timestamp of the page's final entity.
     */
    @NonNull Instant lastModified;
    /**
     * The stable URL of this page itself. This URL must always resolve to this page, even as new entities are added to
     * the feed.
     */
    @NonNull Link.Self self;
    /**
     * The URL of the immediately previous (i.e. older) feed page. If this header is missing then there are no older
     * pages and this page marks the start of the feed.
     */
    @NonNull Optional<Link.@NonNull Prev> prev;
    /**
     * The URL of the immediately next (i.e. newer) feed page. If this header is missing then there are no newer
     * pages and this is currently the latest page of the feed.
     */
    @NonNull Optional<Link.@NonNull Next> next;
    /**
     * Additional unstructured headers.
     */
    @NonNull HttpHeaders extraHeaders;

    public FeedPageHeader(@NonNull final Instant lastModified,
                          @NonNull final Link.Self self,
                          @NonNull final Optional<Link.@NonNull Prev> prev,
                          @NonNull final Optional<Link.@NonNull Next> next) {
        this(lastModified, self, prev, next, HttpHeaders.EMPTY);
    }

    @Override
    public @NonNull HttpHeaders toHttpHeaders() {
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
