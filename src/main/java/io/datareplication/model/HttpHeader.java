package io.datareplication.model;

import lombok.NonNull;
import lombok.Value;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * An HTTP header consisting of a name and one or more values.
 */
@Value
public class HttpHeader {
    /**
     * The normalized header name for case-insensitive comparisons.
     */
    @NonNull String name;
    // TODO: exclude from equals and hashCode?
    /**
     * The header name with its original capitalization for prettier printing.
     */
    @NonNull String displayName;
    @NonNull List<@NonNull String> values;

    @NonNull
    public static final String LAST_MODIFIED = "Last-Modified";
    @NonNull
    public static final String CONTENT_TYPE = "Content-Type";
    @NonNull
    public static final String CONTENT_LENGTH = "Content-Length";
    @NonNull
    public static final String CONTENT_ID = "Content-ID";
    @NonNull
    public static final String OPERATION_TYPE = "Operation-Type";
    @NonNull
    public static final String LINK = "Link";
    @NonNull
    public static final String AUTHORIZATION = "Authorization";

    private static final DateTimeFormatter HTTP_HEADER_FORMATTER = DateTimeFormatter
        .ofPattern("EEE, dd MMM yyyy HH:mm:ss z", Locale.ENGLISH)
        .withZone(ZoneId.of("GMT"));

    private HttpHeader(@NonNull String name, @NonNull List<@NonNull String> values) {
        this.displayName = name;
        this.name = name.toLowerCase(Locale.ENGLISH);
        this.values = List.copyOf(values);
    }

    /**
     * Append a single value to this header.
     *
      * @param value the new value to append
     * @return a new HttpHeader with the given value appended
     */
    public @NonNull HttpHeader append(@NonNull String value) {
        final ArrayList<String> newValues = new ArrayList<>(values);
        newValues.add(value);
        return HttpHeader.of(displayName, newValues);
    }

    /**
     * Append multiple value to this header.
     *
     * @param values the values to append
     * @return a new HttpHeader with the given values appended
     */
    public @NonNull HttpHeader append(@NonNull List<@NonNull String> values) {
        final ArrayList<String> newValues = new ArrayList<>(this.values);
        newValues.addAll(values);
        return HttpHeader.of(displayName, newValues);
    }

    /**
     * Create a header with the given name and single value.
     *
     * @param name  the header name
     * @param value the single header value
     * @return the HttpHeader for the given name and value
     */
    public static @NonNull HttpHeader of(@NonNull String name, @NonNull String value) {
        return new HttpHeader(name, Collections.singletonList(value));
    }

    /**
     * Create a header with the given name and values.
     *
     * @param name   the header name
     * @param values the list of header values
     * @return the HttpHeader for the given name and value
     */
    public static @NonNull HttpHeader of(@NonNull String name, @NonNull List<@NonNull String> values) {
        return new HttpHeader(name, values);
    }

    /**
     * Create a <code>Content-Type</code> header from the given {@link ContentType}.
     *
     * @param contentType the content type
     * @return the <code>Content-Type</code> HttpHeader
     */
    public static @NonNull HttpHeader contentType(@NonNull ContentType contentType) {
        return HttpHeader.of(CONTENT_TYPE, contentType.value());
    }

    /**
     * Create a <code>Content-Length</code> header.
     *
     * @param contentLength the content length value
     * @return the <code>Content-Length</code> HttpHeader
     */
    public static @NonNull HttpHeader contentLength(long contentLength) {
        return HttpHeader.of(CONTENT_LENGTH, Long.toString(contentLength));
    }

    /**
     * Create a <code>Last-Modified</code> header from the given timestamp. The timestamp is formatted in the preferred
     * HTTP datetime header format. Example: <code>Wed, 04 Oct 2023 08:25:33 GMT</code>
     *
     * @param lastModified the timestamp
     * @return the <code>Last-Modified</code> HttpHeader
     */
    public static @NonNull HttpHeader lastModified(@NonNull Timestamp lastModified) {
        return HttpHeader.of(LAST_MODIFIED, HTTP_HEADER_FORMATTER.format(lastModified.value()));
    }
}
