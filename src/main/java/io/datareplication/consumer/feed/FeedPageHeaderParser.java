package io.datareplication.consumer.feed;

import io.datareplication.consumer.PageFormatException;
import io.datareplication.internal.http.HeaderFieldValue;
import io.datareplication.model.HttpHeader;
import io.datareplication.model.HttpHeaders;
import io.datareplication.model.Url;
import io.datareplication.model.feed.ContentId;
import io.datareplication.model.feed.FeedEntityHeader;
import io.datareplication.model.feed.FeedPageHeader;
import io.datareplication.model.feed.Link;
import io.datareplication.model.feed.OperationType;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Optional;
import java.util.function.Function;

import static io.datareplication.model.HttpHeader.CONTENT_ID;
import static io.datareplication.model.HttpHeader.LAST_MODIFIED;
import static io.datareplication.model.HttpHeader.LINK;
import static io.datareplication.model.HttpHeader.OPERATION_TYPE;

/**
 * Parse the headers of a feed page.
 * <p>
 * This class is package-private because it is only used by {@link FeedConsumerImpl}.
 * It is not part of the public API.
 * </p>
 *
 * @see FeedConsumerImpl
 */
@AllArgsConstructor(access = AccessLevel.PACKAGE)
class FeedPageHeaderParser {
    /**
     * Parse the headers of a feed page.
     *
     * @param httpHeaders the headers of the feed page
     * @return the parsed headers
     * @throws PageFormatException if the headers are malformed or missing
     */
    public FeedPageHeader feedPageHeader(@NonNull final HttpHeaders httpHeaders) {

        var pageLinkHeader = httpHeaders
            .get(LINK)
            .orElseThrow(() -> new PageFormatException.MissingSelfLinkHeader(httpHeaders));
        return new FeedPageHeader(
            extractLastModified(
                httpHeaders,
                (String lastModified) -> {
                    try {
                        return fromRfc1123String(lastModified);
                    } catch (DateTimeParseException e) {
                        throw new PageFormatException.InvalidLastModifiedHeader(lastModified, e);
                    }
                }
            ).orElseThrow(() -> new PageFormatException.MissingLastModifiedHeader(httpHeaders)),
            fromHeaderValue(pageLinkHeader, "self")
                .map(Link::self)
                .orElseThrow(() -> new PageFormatException.MissingSelfLinkHeader(httpHeaders)),
            fromHeaderValue(pageLinkHeader, "prev").map(Link::prev),
            fromHeaderValue(pageLinkHeader, "next").map(Link::next)
        );
    }

    private static @NonNull Optional<@NonNull Url> fromHeaderValue(
        @NonNull HttpHeader httpHeader,
        @NonNull String rel
    ) {
        return httpHeader
            .values()
            .stream()
            .map(HeaderFieldValue::parse)
            .filter(fieldValue ->
                fieldValue
                    .parameter("rel")
                    .map(rel::equals)
                    .orElse(false)
            )
            .findFirst()
            .map(FeedPageHeaderParser::toUrl)
            .filter(Optional::isPresent)
            .flatMap(Function.identity());
    }

    private static @NonNull Optional<@NonNull Url> toUrl(@NonNull HeaderFieldValue headerFieldValue) {
        var cleanUrl = headerFieldValue
            .mainValue()
            .trim()
            .replaceFirst("<", "")
            .replaceFirst(">", "");
        if (cleanUrl.isBlank()) {
            return Optional.empty();
        } else {
            return Optional.of(Url.of(cleanUrl));
        }
    }

    /**
     * Parse the headers of a feed entity.
     *
     * @param index       the index of the entity in the page
     * @param httpHeaders the headers of the feed entity
     * @return the parsed headers
     * @throws PageFormatException if the headers are malformed or missing
     */
    public FeedEntityHeader feedEntityHeader(
        @NonNull final Integer index,
        @NonNull final HttpHeaders httpHeaders
    ) {
        return new FeedEntityHeader(
            extractLastModified(
                httpHeaders,
                (String lastModified) -> {
                    try {
                        return fromRfc1123String(lastModified);
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
        Instant parse(@NonNull String input);
    }

    @FunctionalInterface
    private interface OperationTypeParser {
        OperationType parse(@NonNull String input);
    }

    private static @NonNull Optional<@NonNull ContentId> extractContentId(
        @NonNull final HttpHeaders httpHeaders
    ) {
        return httpHeaders.get(CONTENT_ID).flatMap(httpHeader ->
            httpHeader.values().stream().findFirst()).map(ContentId::of);
    }

    private static @NonNull Optional<@NonNull OperationType> extractOperationType(
        final @NonNull HttpHeaders httpHeaders,
        final @NonNull OperationTypeParser operationTypeParser
    ) {
        return httpHeaders.get(OPERATION_TYPE).flatMap(httpHeader ->
            httpHeader.values().stream().findFirst())
            .map(value -> value.replace("http-equiv=", ""))
            .map(operationTypeParser::parse);
    }

    private static @NonNull Optional<@NonNull Instant> extractLastModified(
        @NonNull final HttpHeaders header,
        @NonNull final TimestampParser timestampParser
    ) {
        return header
            .get(LAST_MODIFIED)
            .flatMap(httpHeader -> httpHeader.values().stream().findFirst())
            .map(timestampParser::parse);
    }

    private static @NonNull Instant fromRfc1123String(@NonNull String string) {
        return Instant.from(DateTimeFormatter.RFC_1123_DATE_TIME.parse(string));
    }
}
