package uk.police.k9.dogs.exception;

import io.micronaut.context.annotation.Replaces;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.core.util.StringUtils;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Produces;
import io.micronaut.http.server.exceptions.ExceptionHandler;
import io.micronaut.http.server.exceptions.response.Error;
import io.micronaut.http.server.exceptions.response.ErrorContext;
import io.micronaut.http.server.exceptions.response.ErrorResponseProcessor;
import io.micronaut.validation.exceptions.ConstraintExceptionHandler;
import jakarta.inject.Singleton;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.ElementKind;
import jakarta.validation.Path;

import java.util.Comparator;
import java.util.List;
import java.util.StringJoiner;

/**
 * Turns a failed {@code @Valid} request into {@code 400}, listing every rejected field separately
 * rather than flattening the violations into sentences as Micronaut's own handler does. A
 * constraint spanning several fields has no field to blame, so its message becomes the headline.
 */
@Singleton
@Replaces(ConstraintExceptionHandler.class)
@Produces(MediaType.APPLICATION_JSON)
public class ValidationExceptionHandler
        implements ExceptionHandler<ConstraintViolationException, HttpResponse<?>> {

    private static final String VALIDATION_FAILED = "The request failed validation";

    private final ErrorResponseProcessor<?> errorResponseProcessor;

    public ValidationExceptionHandler(ErrorResponseProcessor<?> errorResponseProcessor) {
        this.errorResponseProcessor = errorResponseProcessor;
    }

    @Override
    public HttpResponse<?> handle(HttpRequest request, ConstraintViolationException exception) {
        List<Error> errors = exception.getConstraintViolations().stream()
                .<Error>map(ValidationExceptionHandler::toError)
                // Violations arrive in an unspecified order; sorting keeps the response stable.
                .sorted(Comparator.comparing(error -> error.getPath().orElse("")))
                .toList();

        ErrorContext.Builder context = ErrorContext.builder(request).cause(exception).errors(errors);
        if (errors.isEmpty()) {
            context.errorMessage(StringUtils.isNotEmpty(exception.getMessage())
                    ? exception.getMessage() : VALIDATION_FAILED);
        } else if (errors.stream().allMatch(error -> error.getPath().isPresent())) {
            // Every failure names a field, so the headline message has to be supplied.
            context.errorMessage(VALIDATION_FAILED);
        }
        return errorResponseProcessor.processResponse(context.build(), HttpResponse.badRequest());
    }

    private static RequestError toError(ConstraintViolation<?> violation) {
        return new RequestError(field(violation.getPropertyPath()), violation.getMessage());
    }

    /**
     * A controller argument gives a path like {@code create.request.birthDate}; only the trailing
     * property names mean anything outside. A cross-field constraint leaves nothing behind, which
     * is how it is recognised.
     */
    @Nullable
    private static String field(Path propertyPath) {
        StringJoiner field = new StringJoiner(".");
        for (Path.Node node : propertyPath) {
            if (node.getName() != null && node.getKind() != ElementKind.METHOD
                    && node.getKind() != ElementKind.CONSTRUCTOR
                    && node.getKind() != ElementKind.PARAMETER
                    && node.getKind() != ElementKind.CROSS_PARAMETER
                    && node.getKind() != ElementKind.RETURN_VALUE) {
                field.add(node.getName());
            }
        }
        return field.length() == 0 ? null : field.toString();
    }
}
