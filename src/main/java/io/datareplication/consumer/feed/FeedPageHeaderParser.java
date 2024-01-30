package io.datareplication.consumer.feed;

import io.datareplication.consumer.PageFormatException;
import io.datareplication.internal.http.HeaderFieldValue;
import io.datareplication.model.HttpHeader;
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
import java.util.Optional;
import java.util.function.Function;

import static io.datareplication.model.HttpHeader.CONTENT_ID;
import static io.datareplication.model.HttpHeader.LAST_MODIFIED;
import static io.datareplication.model.HttpHeader.LINK;
import static io.datareplication.model.HttpHeader.OPERATION_TYPE;

@AllArgsConstructor(access = AccessLevel.PACKAGE)
public class FeedPageHeaderParser {
    public FeedPageHeader feedPageHeader(final HttpHeaders httpHeaders) {

        var pageLinkHeader = httpHeaders
            .get(LINK)
            .orElseThrow(() -> new PageFormatException.MissingLinkHeader(httpHeaders));
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
            ).orElseThrow(() -> new PageFormatException.MissingLastModifiedHeader(httpHeaders)),
            fromHeaderValue(pageLinkHeader, "self").map(Link::self)
                .orElseThrow(() -> new PageFormatException.MissingSelfLinkHeader(httpHeaders)),
            fromHeaderValue(pageLinkHeader, "prev").map(Link::prev),
            fromHeaderValue(pageLinkHeader, "next").map(Link::next)
        );
    }

    private static Optional<Url> fromHeaderValue(HttpHeader httpHeader, String rel) {
        return httpHeader
            .values()
            .stream()
            .filter(headerFieldValue -> headerFieldValue.contains("; rel=" + rel))
            .map(HeaderFieldValue::parse)
            .map(FeedPageHeaderParser::toUrl)
            .filter(Optional::isPresent)
            .findFirst()
            .flatMap(Function.identity());
    }

    private static Optional<Url> toUrl(HeaderFieldValue headerFieldValue) {
        var cleanUrl = headerFieldValue
            .mainValue()
            .trim()
            .replaceFirst("<", "")
            .replaceFirst(">", "");
        if (!cleanUrl.isBlank()) {
            return Optional.of(Url.of(cleanUrl));
        } else {
            return Optional.empty();
        }
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
            extractContentId(httpHeaders).orElseThrow(() -> new PageFormatException.MissingContentIdInEntity(index)));
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
}
