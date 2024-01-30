package io.datareplication.consumer.feed;

import io.datareplication.consumer.PageFormatException;
import io.datareplication.model.HttpHeader;
import io.datareplication.model.HttpHeaders;
import io.datareplication.model.Timestamp;
import io.datareplication.model.Url;
import io.datareplication.model.feed.ContentId;
import io.datareplication.model.feed.FeedEntityHeader;
import io.datareplication.model.feed.FeedPageHeader;
import io.datareplication.model.feed.Link;
import io.datareplication.model.feed.OperationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FeedPageHeaderParserTest {
    private static final String LAST_MODIFIED = "Thu, 5 Oct 2023 03:00:14 GMT";
    private static final String ANY_CONTENT_ID = "content-id-1234";
    private static final String OPERATION_TYPE = "PUT";
    private static final String LINK_PREV = "<https://example.datareplication.io/1>; rel=prev";
    private static final String LINK_SELF = "<https://example.datareplication.io/2>; rel=self";
    private static final String LINK_NEXT = "<https://example.datareplication.io/3>; rel=next";

    private FeedPageHeaderParser feedPageHeaderParser;

    @BeforeEach
    void setUp() {
        feedPageHeaderParser = new FeedPageHeaderParser();
    }

    @Test
    void shouldParsePageHttpHeader() {
        HttpHeader lastModifiedHttpHeader = HttpHeader.of(HttpHeader.LAST_MODIFIED, LAST_MODIFIED);
        HttpHeader linkSelfHttpHeader = HttpHeader.of(HttpHeader.LINK, LINK_SELF);
        HttpHeaders httpHeaders = HttpHeaders.of(lastModifiedHttpHeader, linkSelfHttpHeader);

        FeedPageHeader feedPageHeader = feedPageHeaderParser.feedPageHeader(httpHeaders);

        assertThat(feedPageHeader).isEqualTo(
            new FeedPageHeader(
                Timestamp.fromRfc1123String(LAST_MODIFIED),
                Link.self(Url.of("https://example.datareplication.io/2")),
                Optional.empty(),
                Optional.empty()
            )
        );
    }

    @Test
    void shouldParsePageHttpHeaderWithPrevNextLinks() {
        HttpHeader lastModifiedHttpHeader = HttpHeader.of(HttpHeader.LAST_MODIFIED, LAST_MODIFIED);
        HttpHeader linkSelfHttpHeader = HttpHeader.of(HttpHeader.LINK, LINK_SELF);
        HttpHeader linkPrevHttpHeader = HttpHeader.of(HttpHeader.LINK, LINK_PREV);
        HttpHeader linkNextHttpHeader = HttpHeader.of(HttpHeader.LINK, LINK_NEXT);
        HttpHeaders httpHeaders = HttpHeaders.of(
            lastModifiedHttpHeader,
            linkSelfHttpHeader,
            linkPrevHttpHeader,
            linkNextHttpHeader
        );

        FeedPageHeader feedPageHeader = feedPageHeaderParser.feedPageHeader(httpHeaders);

        assertThat(feedPageHeader).isEqualTo(
            new FeedPageHeader(
                Timestamp.fromRfc1123String(LAST_MODIFIED),
                Link.self(Url.of("https://example.datareplication.io/2")),
                Optional.of(Link.prev(Url.of("https://example.datareplication.io/1"))),
                Optional.of(Link.next(Url.of("https://example.datareplication.io/3")))
            )
        );
    }

    @Test
    void noLinkHttpHeaderShouldThrowException() {
        HttpHeader lastModifiedHttpHeader = HttpHeader.of(HttpHeader.LAST_MODIFIED, LAST_MODIFIED);
        HttpHeaders httpHeaders = HttpHeaders.of(lastModifiedHttpHeader);

        var missingSelfLinkHeader = assertThrows(
            PageFormatException.MissingLinkHeader.class,
            () -> feedPageHeaderParser.feedPageHeader(httpHeaders)
        );

        assertThat(missingSelfLinkHeader)
            .hasMessage("Link header is missing from HTTP response: '" + httpHeaders + "'");
    }

    @Test
    void missingSelfHttpHeaderShouldThrowException() {
        HttpHeader lastModifiedHttpHeader = HttpHeader.of(HttpHeader.LAST_MODIFIED, LAST_MODIFIED);
        HttpHeader linkNextHttpHeader = HttpHeader.of(HttpHeader.LINK, LINK_NEXT);
        HttpHeaders httpHeaders = HttpHeaders.of(lastModifiedHttpHeader, linkNextHttpHeader);

        var missingSelfLinkHeader = assertThrows(
            PageFormatException.MissingSelfLinkHeader.class,
            () -> feedPageHeaderParser.feedPageHeader(httpHeaders)
        );

        assertThat(missingSelfLinkHeader)
            .hasMessage("LINK; rel=self header is missing from HTTP response: '" + httpHeaders + "'");
    }

    @Test
    void missingLastModifiedShouldThrowException() {
        HttpHeader linkSelfHttpHeader = HttpHeader.of(HttpHeader.LINK, LINK_SELF);
        HttpHeaders httpHeaders = HttpHeaders.of(linkSelfHttpHeader);

        var missingLastModified = assertThrows(
            PageFormatException.MissingLastModifiedHeader.class,
            () -> feedPageHeaderParser.feedPageHeader(httpHeaders)
        );

        assertThat(missingLastModified)
            .hasMessage("Last-Modified header is missing from HTTP response: '" + httpHeaders + "'");
    }

    @Test
    void invalidLastModifiedShouldThrowException() {
        HttpHeader lastModifiedHttpHeader = HttpHeader.of(HttpHeader.LAST_MODIFIED, "not a valid date");
        HttpHeader linkSelfHttpHeader = HttpHeader.of(HttpHeader.LINK, LINK_SELF);
        HttpHeaders httpHeaders = HttpHeaders.of(lastModifiedHttpHeader, linkSelfHttpHeader);

        var missingLastModifiedInEntity = assertThrows(
            PageFormatException.InvalidLastModifiedHeader.class,
            () -> feedPageHeaderParser.feedPageHeader(httpHeaders)
        );

        assertThat(missingLastModifiedInEntity)
            .hasMessage("Last-Modified header is invalid: 'not a valid date'");
    }

    @Test
    void shouldParseFeedEntityHttpHeader() {
        HttpHeader lastModifiedHttpHeader = HttpHeader.of(HttpHeader.LAST_MODIFIED, LAST_MODIFIED);
        HttpHeader contentIdHttpHeader = HttpHeader.of(HttpHeader.CONTENT_ID, ANY_CONTENT_ID);
        HttpHeader operationTypeHttpHeader = HttpHeader.of(HttpHeader.OPERATION_TYPE, OPERATION_TYPE);
        HttpHeaders httpHeaders = HttpHeaders.of(lastModifiedHttpHeader, contentIdHttpHeader, operationTypeHttpHeader);

        FeedEntityHeader feedEntityHeader = feedPageHeaderParser.feedEntityHeader(1, httpHeaders);

        assertThat(feedEntityHeader).isEqualTo(
            new FeedEntityHeader(
                Timestamp.fromRfc1123String(LAST_MODIFIED),
                OperationType.valueOf(OPERATION_TYPE),
                ContentId.of(ANY_CONTENT_ID)
            )
        );
    }

    @Test
    void missingContentIdShouldThrowException() {
        HttpHeader lastModifiedHttpHeader = HttpHeader.of(HttpHeader.LAST_MODIFIED, LAST_MODIFIED);
        HttpHeader operationTypeHttpHeader = HttpHeader.of(HttpHeader.OPERATION_TYPE, OPERATION_TYPE);
        HttpHeaders httpHeaders = HttpHeaders.of(lastModifiedHttpHeader, operationTypeHttpHeader);

        var missingContentIdInEntity = assertThrows(
            PageFormatException.MissingContentIdInEntity.class,
            () -> feedPageHeaderParser.feedEntityHeader(1, httpHeaders)
        );

        assertThat(missingContentIdInEntity)
            .hasMessage("Content-Id header is missing from entity at index 1");
    }

    @Test
    void missingLastModifiedInEntityShouldThrowException() {
        HttpHeader contentIdHttpHeader = HttpHeader.of(HttpHeader.CONTENT_ID, ANY_CONTENT_ID);
        HttpHeader operationTypeHttpHeader = HttpHeader.of(HttpHeader.OPERATION_TYPE, OPERATION_TYPE);
        HttpHeaders httpHeaders = HttpHeaders.of(contentIdHttpHeader, operationTypeHttpHeader);

        var missingLastModifiedInEntity = assertThrows(
            PageFormatException.MissingLastModifiedHeaderInEntity.class,
            () -> feedPageHeaderParser.feedEntityHeader(1, httpHeaders)
        );

        assertThat(missingLastModifiedInEntity)
            .hasMessage("Last-Modified header is missing from entity at index 1");
    }

    @Test
    void invalidLastModifiedInEntityShouldThrowException() {
        HttpHeader lastModifiedHttpHeader = HttpHeader.of(HttpHeader.LAST_MODIFIED, "not a valid date");
        HttpHeader contentIdHttpHeader = HttpHeader.of(HttpHeader.CONTENT_ID, ANY_CONTENT_ID);
        HttpHeader operationTypeHttpHeader = HttpHeader.of(HttpHeader.OPERATION_TYPE, OPERATION_TYPE);
        HttpHeaders httpHeaders = HttpHeaders.of(lastModifiedHttpHeader, contentIdHttpHeader, operationTypeHttpHeader);

        var missingLastModifiedInEntity = assertThrows(
            PageFormatException.InvalidLastModifiedHeaderInEntity.class,
            () -> feedPageHeaderParser.feedEntityHeader(1, httpHeaders)
        );

        assertThat(missingLastModifiedInEntity)
            .hasMessage("unparseable Last-Modified header from entity at index 1: 'not a valid date'");
    }


    @Test
    void missingOperationTypeShouldThrowException() {
        HttpHeader contentIdHttpHeader = HttpHeader.of(HttpHeader.CONTENT_ID, ANY_CONTENT_ID);
        HttpHeader lastModifiedHttpHeader = HttpHeader.of(HttpHeader.LAST_MODIFIED, LAST_MODIFIED);
        HttpHeaders httpHeaders = HttpHeaders.of(contentIdHttpHeader, lastModifiedHttpHeader);

        var missingOperationTypeInEntity = assertThrows(
            PageFormatException.MissingOperationTypeInEntity.class,
            () -> feedPageHeaderParser.feedEntityHeader(1, httpHeaders)
        );

        assertThat(missingOperationTypeInEntity)
            .hasMessage("Operation-Type header is missing from entity at index 1");
    }

    @Test
    void unknownOperationTypeShouldThrowException() {
        HttpHeader lastModifiedHttpHeader = HttpHeader.of(HttpHeader.LAST_MODIFIED, LAST_MODIFIED);
        HttpHeader contentIdHttpHeader = HttpHeader.of(HttpHeader.CONTENT_ID, ANY_CONTENT_ID);
        HttpHeader operationTypeHttpHeader = HttpHeader.of(HttpHeader.OPERATION_TYPE, "CUT");
        HttpHeaders httpHeaders = HttpHeaders.of(lastModifiedHttpHeader, contentIdHttpHeader, operationTypeHttpHeader);

        var missingOperationTypeInEntity = assertThrows(
            PageFormatException.UnparseableOperationTypeInEntity.class,
            () -> feedPageHeaderParser.feedEntityHeader(1, httpHeaders)
        );

        assertThat(missingOperationTypeInEntity)
            .hasMessage("unparseable Operation-Type header from entity at index 1: 'CUT'");
    }
}
