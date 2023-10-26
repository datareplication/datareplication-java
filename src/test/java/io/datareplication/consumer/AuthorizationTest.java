package io.datareplication.consumer;

import io.datareplication.model.HttpHeader;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AuthorizationTest {
    @Test
    void toHeader_shouldBuildHeaderFromSchemeAndParameters() {
        final var auth = Authorization.of("Some-Scheme", "params,params,params");

        assertThat(auth.toHeader())
            .isEqualTo(HttpHeader.of("Authorization", "Some-Scheme params,params,params"));
    }

    @Test
    void basic_shouldBase64encodeUsernameAndPassword() {
        // example from MDN:
        // https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Authorization#basic_authentication
        final var auth = Authorization.basic("aladdin", "opensesame");

        assertThat(auth)
            .isEqualTo(Authorization.of("Basic", "YWxhZGRpbjpvcGVuc2VzYW1l"));
    }
}
