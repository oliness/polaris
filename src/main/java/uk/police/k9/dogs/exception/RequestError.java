package uk.police.k9.dogs.exception;

import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.server.exceptions.response.Error;

import java.util.Optional;

/**
 * One thing that was wrong with a request, in the shape Micronaut's error pipeline understands.
 * An error naming a field becomes an entry in {@code details}; one without becomes the headline
 * message.
 */
record RequestError(@Nullable String field, String message) implements Error {

    @Override
    public Optional<String> getPath() {
        return Optional.ofNullable(field);
    }

    @Override
    public String getMessage() {
        return message;
    }
}
