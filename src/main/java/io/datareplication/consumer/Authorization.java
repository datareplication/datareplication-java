package io.datareplication.consumer;

import io.datareplication.model.HttpHeader;
import lombok.NonNull;
import lombok.Value;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Value(staticConstructor = "of")
public class Authorization {
    @NonNull String scheme;
    @NonNull String parameters;

    public @NonNull HttpHeader toHeader() {
        return HttpHeader.of(HttpHeader.AUTHORIZATION, String.format("%s %s", scheme, parameters));
    }

    public static @NonNull Authorization basic(@NonNull String username, @NonNull String password) {
        String combined = String.format("%s:%s", username, password);
        byte[] encoded = Base64.getEncoder().encode(combined.getBytes(StandardCharsets.UTF_8));
        String parameters = new String(encoded, StandardCharsets.UTF_8);
        return Authorization.of("Basic", parameters);
    }
}
