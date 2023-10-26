package io.datareplication.internal.http;

import io.datareplication.consumer.Authorization;
import io.datareplication.model.Url;
import lombok.NonNull;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Return Authorization for an HTTP request based on the URL. Mostly just a helper interface to simplify type signatures
 * and have a place to stash the utility constructors.
 */
@FunctionalInterface
public interface AuthSupplier extends Function<Url, Optional<Authorization>> {
    /**
     * @return an AuthSupplier that always returns {@link Optional#empty()}
     */
    static AuthSupplier none() {
        return (ignored) -> Optional.empty();
    }

    /**
     * @param authorization authorization
     * @return an AuthSupplier that always returns <code>authorization</code>
     */
    static AuthSupplier constant(@NonNull Authorization authorization) {
        return (ignored) -> Optional.of(authorization);
    }

    /**
     * @param authorization a function that returns Authorization
     * @return an AuthSupplier that calls the supplied function whenever the AuthSupplier is called itself
     */
    static AuthSupplier supplier(@NonNull Supplier<Authorization> authorization) {
        return (ignored) -> Optional.ofNullable(authorization.get());
    }
}
