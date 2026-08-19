package uk.police.k9.dogs.exception;

import io.micronaut.core.util.StringUtils;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.server.exceptions.response.Error;
import io.micronaut.http.server.exceptions.response.ErrorContext;
import io.micronaut.http.server.exceptions.response.JsonErrorResponseBodyProvider;
import jakarta.inject.Singleton;

import java.util.List;

/**
 * Builds the body of every failed response as an {@link ApiError}. Micronaut routes its own errors
 * here and the application's handlers delegate here too, so the API answers with one shape
 * throughout; declaring the bean also switches off the framework's HAL-style default.
 */
@Singleton
public class ApiErrorBodyProvider implements JsonErrorResponseBodyProvider<ApiError> {

    private static final String VALIDATION_FAILED = "The request failed validation";

    @Override
    public ApiError body(ErrorContext errorContext, HttpResponse<?> response) {
        List<ApiError.FieldError> details = errorContext.getErrors().stream()
                .filter(error -> error.getPath().isPresent())
                .map(error -> new ApiError.FieldError(error.getPath().get(), error.getMessage()))
                .toList();

        return ApiError.of(response.status(), message(errorContext, response.status(), details),
                errorContext.getRequest().getPath(), details);
    }

    /** The first error not belonging to a field, falling back to a summary and then the status. */
    private static String message(ErrorContext errorContext, HttpStatus status,
                                  List<ApiError.FieldError> details) {
        return errorContext.getErrors().stream()
                .filter(error -> error.getPath().isEmpty())
                .map(Error::getMessage)
                .filter(StringUtils::isNotEmpty)
                .findFirst()
                .orElseGet(() -> details.isEmpty() ? status.getReason() : VALIDATION_FAILED);
    }
}
