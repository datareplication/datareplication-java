package io.datareplication.consumer.feed;

import io.datareplication.consumer.PageFormatException.MissingContentIdInEntity;
import io.datareplication.consumer.PageFormatException.MissingLastModifiedHeader;
import io.datareplication.consumer.PageFormatException.MissingLastModifiedHeaderInEntity;
import io.datareplication.consumer.PageFormatException.MissingLinkHeader;
import io.datareplication.consumer.PageFormatException.MissingOperationTypeInEntity;
import io.datareplication.consumer.PageFormatException.MissingSelfLinkHeader;
import io.datareplication.consumer.StreamingPage;
import io.datareplication.internal.page.PageLoader;
import io.datareplication.internal.page.WrappedStreamingPage;
import io.datareplication.model.Entity;
import io.datareplication.model.HttpHeaders;
import io.datareplication.model.Timestamp;
import io.datareplication.model.Url;
import io.datareplication.model.feed.ContentId;
import io.datareplication.model.feed.FeedEntityHeader;
import io.datareplication.model.feed.FeedPageHeader;
import io.datareplication.model.feed.Link;
import io.datareplication.model.feed.OperationType;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import reactor.adapter.JdkFlowAdapter;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Flow;
import java.util.stream.Collectors;

import static io.datareplication.model.HttpHeader.CONTENT_ID;
import static io.datareplication.model.HttpHeader.LAST_MODIFIED;
import static io.datareplication.model.HttpHeader.LINK;
import static io.datareplication.model.HttpHeader.OPERATION_TYPE;

@AllArgsConstructor(access = AccessLevel.PACKAGE)
public class FeedConsumerImpl implements FeedConsumer {
    private final PageLoader pageLoader;
    private final FeedPageCrawler feedPageCrawler;

    @Override
    public @NonNull Flow.Publisher<
        @NonNull StreamingPage<@NonNull FeedPageHeader, @NonNull FeedEntityHeader>
        > streamPages(@NonNull final Url url,
                      @NonNull final StartFrom startFrom) {
        // TODO: NextLinks
        Mono<StreamingPage<FeedPageHeader, FeedEntityHeader>> pageMono = Mono
            .fromCompletionStage(feedPageCrawler.crawl(url, startFrom))
            .flatMap(feedPageHeader -> pageLoader.load(feedPageHeader.self().value()))
            .map(this::wrapPage);
        return JdkFlowAdapter.publisherToFlowPublisher(pageMono);
    }

    @Override
    public @NonNull Flow.Publisher<
        @NonNull Entity<@NonNull FeedEntityHeader>
        > streamEntities(@NonNull final Url url,
                         @NonNull final StartFrom startFrom) {
        throw new UnsupportedOperationException("NIY");
    }

    private StreamingPage<FeedPageHeader, FeedEntityHeader> wrapPage(
        StreamingPage<HttpHeaders, HttpHeaders> page
    ) {
        HttpHeaders pageHttpHeaders = page.header();
        var pageLinkHeaders = pageHttpHeaders
            .get(LINK)
            .orElseThrow(MissingLinkHeader::new);
        var pageLinkMap = toMap(pageLinkHeaders.values());


        return new WrappedStreamingPage<>(page,
            new FeedPageHeader(
                extractLastModified(pageHttpHeaders).orElseThrow(MissingLastModifiedHeader::new),
                extractSelfLink(pageLinkMap).orElseThrow(() -> new MissingSelfLinkHeader(pageHttpHeaders)),
                extractPrevLink(pageLinkMap),
                extractNextLink(pageLinkMap)),
            (index, httpHeaders) -> new FeedEntityHeader(
                extractLastModified(httpHeaders).orElseThrow(() -> new MissingLastModifiedHeaderInEntity(index)),
                extractOperationType(httpHeaders).orElseThrow(() -> new MissingOperationTypeInEntity(index)),
                extractContentId(httpHeaders).orElseThrow(() -> new MissingContentIdInEntity(index)))
        );
    }

    private static @NonNull Optional<ContentId> extractContentId(final HttpHeaders httpHeaders) {
        return httpHeaders.get(CONTENT_ID).flatMap(httpHeader ->
            httpHeader.values().stream().findFirst()).map(ContentId::of);
    }

    private static @NonNull Optional<OperationType> extractOperationType(final HttpHeaders httpHeaders) {
        // TODO: Handle IllegalArgumentException of OperationType::valueOf
        return httpHeaders.get(OPERATION_TYPE).flatMap(httpHeader ->
            httpHeader.values().stream().findFirst()).map(OperationType::valueOf);
    }

    private static @NonNull Optional<Timestamp> extractLastModified(final HttpHeaders header) {
        return header
            .get(LAST_MODIFIED)
            .flatMap(httpHeader -> httpHeader.values().stream().findFirst())
            .map(Timestamp::fromRfc1123String);
    }

    private static @NonNull Optional<Link.@NonNull Self> extractSelfLink(final Map<Link.Rel, Url> pageLinkMap) {
        return Optional
            .ofNullable(pageLinkMap.getOrDefault(Link.Rel.SELF, null))
            .map(Link::self);
    }

    private static @NonNull Optional<Link.@NonNull Next> extractNextLink(final Map<Link.Rel, Url> pageLinkMap) {
        return Optional
            .ofNullable(pageLinkMap.getOrDefault(Link.Rel.NEXT, null))
            .map(Link::next);
    }

    private static @NonNull Optional<Link.@NonNull Prev> extractPrevLink(final Map<Link.Rel, Url> pageLinkMap) {
        return Optional
            .ofNullable(pageLinkMap.getOrDefault(Link.Rel.PREV, null))
            .map(Link::prev);
    }

    private static @NonNull Map<Link.@NonNull Rel, @NonNull Url> toMap(final List<String> values) {
        return values
            .stream()
            .collect(Collectors.toMap(
                x -> extractRel(x).orElseThrow(IllegalArgumentException::new),
                x -> extractUrl(x).orElseThrow(IllegalArgumentException::new)));
    }

    // TODO: Refactor whole Link parsing (guided by tests)
    private static @NonNull Optional<@NonNull Url> extractUrl(final String value) {
        return Optional.of(Url.of(value.replaceFirst(".*; rel=", "").trim()));
    }

    private static @NonNull Optional<Link.@NonNull Rel> extractRel(final String headerValue) {
        // TODO: Handle IllegalArgumentException of Link.Rel::valueOf
        if (headerValue.contains("rel=\"")) {
            var relString = headerValue
                .replaceAll(".*rel=\"", "")
                .replace("\"", "");
            return Optional.of(relString).map(Link.Rel::valueOf);
        } else if (headerValue.contains("rel=")) {
            var relString = headerValue
                .replaceAll(".*rel=\"", "");
            return Optional.of(relString).map(Link.Rel::valueOf);
        } else {
            return Optional.empty();
        }
    }
}
