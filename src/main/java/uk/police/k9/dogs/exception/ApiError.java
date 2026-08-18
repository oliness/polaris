package uk.police.k9.dogs.exception;

import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.HttpStatus;
import io.micronaut.serde.annotation.Serdeable;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

/** The single error shape returned by every endpoint, so a client parses only one thing. */
@Serdeable
@Schema(name = "ApiError", description = "The error shape returned by every endpoint")
public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String message,
        @Nullable String path,
        @Nullable List<FieldError> details
) {

    public static ApiError of(HttpStatus status, String message, @Nullable String path) {
        return new ApiError(Instant.now(), status.getCode(), status.getReason(), message, path, null);
    }

    public static ApiError of(HttpStatus status, String message, @Nullable String path,
                              List<FieldError> details) {
        return new ApiError(Instant.now(), status.getCode(), status.getReason(), message, path,
                details.isEmpty() ? null : details);
    }

    /** One rejected field, e.g. {@code birthDate}. */
    @Serdeable
    @Schema(name = "FieldError", description = "A single rejected field")
    public record FieldError(String field, String message) {
    }
}
