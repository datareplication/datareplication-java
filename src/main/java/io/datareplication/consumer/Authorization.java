package io.datareplication.consumer;

import io.datareplication.model.HttpHeader;
import lombok.NonNull;
import lombok.Value;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * The value of an HTTP <code>Authorization</code> header.
 */
@Value(staticConstructor = "of")
public class Authorization {
    /**
     * Specifies the authentication scheme, which determines the format of the {@link #parameters()} field. Common
     * schemes are:
     * <ul>
     *     <li><a href="https://datatracker.ietf.org/doc/html/rfc7617">Basic</a>:
     *     base64-encoded username and password</li>
     *     <li><a href="https://datatracker.ietf.org/doc/html/rfc6750">Bearer</a>: OAuth 2.0</li>
     * </ul>
     */
    @NonNull String scheme;
    /**
     * The parameters for the authorization. The format and contents of this field are entirely determined by the
     * {@link #scheme()}.
     */
    @NonNull String parameters;

    /**
     * Return this Authorization as an {@link HttpHeader}.
     * @return an <code>Authorization</code> {@link HttpHeader} containing the scheme and parameters
     */
    public @NonNull HttpHeader toHeader() {
        return HttpHeader.of(HttpHeader.AUTHORIZATION, String.format("%s %s", scheme, parameters));
    }

    /**
     * <p>Create an Authorization using the Basic scheme with the given username and password.</p>
     *
     * <p>For Basic auth, the username and password are joined with a <code>':'</code> and the resulting string
     * is base64-encoded.</p>
     *
     * @param username the username to authenticate as
     * @param password the password for the given username
     * @return an Authorization with the encoded username and password
     */
    public static @NonNull Authorization basic(@NonNull String username, @NonNull String password) {
        String combined = String.format("%s:%s", username, password);
        byte[] encoded = Base64.getEncoder().encode(combined.getBytes(StandardCharsets.UTF_8));
        String parameters = new String(encoded, StandardCharsets.UTF_8);
        return Authorization.of("Basic", parameters);
    }
}
