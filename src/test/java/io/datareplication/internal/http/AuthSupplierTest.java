package io.datareplication.internal.http;

import io.datareplication.consumer.Authorization;
import io.datareplication.model.Url;
import org.junit.jupiter.api.Test;

import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

class AuthSupplierTest {
    private static final Authorization SOME_AUTH = Authorization.of("Test", "username=test,key=test");

    @Test
    void none_shouldAlwaysReturnOptionalEmpty() {
        final var supplier = AuthSupplier.none();

        assertThat(supplier.apply(Url.of("1"))).isEmpty();
        assertThat(supplier.apply(Url.of("2"))).isEmpty();
    }

    @Test
    void constant_shouldAlwaysReturnTheGivenAuthorization() {
        final var supplier = AuthSupplier.constant(SOME_AUTH);

        assertThat(supplier.apply(Url.of("1"))).contains(SOME_AUTH);
        assertThat(supplier.apply(Url.of("2"))).contains(SOME_AUTH);
    }

    @Test
    void supplier_shouldCallFunctionAgainEachTimeTheSupplierIsCalled() {
        final var counter = new Supplier<Authorization>() {
            private int counter;

            @Override
            public Authorization get() {
                counter++;
                return Authorization.of("Counting", Integer.toString(counter));
            }
        };

        final var supplier = AuthSupplier.supplier(counter);

        assertThat(supplier.apply(Url.of("1"))).contains(Authorization.of("Counting", "1"));
        assertThat(supplier.apply(Url.of("1"))).contains(Authorization.of("Counting", "2"));
        assertThat(supplier.apply(Url.of("2"))).contains(Authorization.of("Counting", "3"));
        assertThat(supplier.apply(Url.of("1"))).contains(Authorization.of("Counting", "4"));
        assertThat(supplier.apply(Url.of("3"))).contains(Authorization.of("Counting", "5"));
    }
}
