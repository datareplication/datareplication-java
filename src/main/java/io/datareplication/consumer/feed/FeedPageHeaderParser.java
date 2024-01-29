package io.datareplication.consumer.feed;

import io.datareplication.consumer.PageFormatException;
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

import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static io.datareplication.model.HttpHeader.CONTENT_ID;
import static io.datareplication.model.HttpHeader.LAST_MODIFIED;
import static io.datareplication.model.HttpHeader.LINK;
import static io.datareplication.model.HttpHeader.OPERATION_TYPE;

@AllArgsConstructor(access = AccessLevel.PACKAGE)
public class FeedPageHeaderParser {
    public FeedPageHeader feedPageHeader(final HttpHeaders httpHeaders) {

        var pageLinkHeaders = httpHeaders
            .get(LINK)
            .orElseThrow(PageFormatException.MissingLinkHeader::new);
        var pageLinkMap = toMap(pageLinkHeaders.values());
        return new FeedPageHeader(
            extractLastModified(
                httpHeaders,
                (String lastModified) -> {
                    try {
                        return Timestamp.fromRfc1123String(lastModified);
                    } catch (DateTimeParseException e) {
                        throw new PageFormatException.InvalidLastModifiedHeader(lastModified, e);
                    }
                }
            ).orElseThrow(PageFormatException.MissingLastModifiedHeader::new),
            extractSelfLink(pageLinkMap).orElseThrow(() -> new PageFormatException.MissingSelfLinkHeader(httpHeaders)),
            extractPrevLink(pageLinkMap),
            extractNextLink(pageLinkMap));
    }

    public FeedEntityHeader feedEntityHeader(final Integer index, final HttpHeaders httpHeaders) {
        return new FeedEntityHeader(
            extractLastModified(
                httpHeaders,
                (String lastModified) -> {
                    try {
                        return Timestamp.fromRfc1123String(lastModified);
                    } catch (DateTimeParseException e) {
                        throw new PageFormatException.InvalidLastModifiedHeaderInEntity(index, lastModified, e);
                    }
                }
            ).orElseThrow(() -> new PageFormatException.MissingLastModifiedHeaderInEntity(index)),
            extractOperationType(
                httpHeaders,
                (String operationType) -> {
                    try {
                        return OperationType.valueOf(operationType);
                    } catch (IllegalArgumentException e) {
                        throw new PageFormatException.UnparseableOperationTypeInEntity(index, operationType, e);
                    }
                }
            ).orElseThrow(() -> new PageFormatException.MissingOperationTypeInEntity(index)),
            extractContentId(httpHeaders).orElseThrow(() -> new PageFormatException.MissingContentIdInEntity(index)))
            ;
    }

    @FunctionalInterface
    private interface TimestampParser {
        Timestamp parse(String input);
    }

    @FunctionalInterface
    private interface OperationTypeParser {
        OperationType parse(String input);
    }

    private static @NonNull Optional<ContentId> extractContentId(final HttpHeaders httpHeaders) {
        return httpHeaders.get(CONTENT_ID).flatMap(httpHeader ->
            httpHeader.values().stream().findFirst()).map(ContentId::of);
    }

    private static @NonNull Optional<OperationType> extractOperationType(
        final HttpHeaders httpHeaders,
        final OperationTypeParser operationTypeParser) {
        return httpHeaders.get(OPERATION_TYPE).flatMap(httpHeader ->
            httpHeader.values().stream().findFirst()).map(operationTypeParser::parse);
    }

    private static @NonNull Optional<Timestamp> extractLastModified(final HttpHeaders header,
                                                                    final TimestampParser timestampParser) {
        return header
            .get(LAST_MODIFIED)
            .flatMap(httpHeader -> httpHeader.values().stream().findFirst())
            .map(timestampParser::parse);
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

    private @NonNull Map<Link.@NonNull Rel, @NonNull Url> toMap(final List<String> values) {
        return values
            .stream()
            .collect(Collectors.toMap(
                x -> extractRel(x).orElseThrow(IllegalArgumentException::new),
                x -> extractUrl(x).orElseThrow(IllegalArgumentException::new)));
    }
}
