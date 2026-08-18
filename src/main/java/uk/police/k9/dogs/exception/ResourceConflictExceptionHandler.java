package uk.police.k9.dogs.exception;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Produces;
import io.micronaut.http.server.exceptions.ExceptionHandler;
import io.micronaut.http.server.exceptions.response.ErrorContext;
import io.micronaut.http.server.exceptions.response.ErrorResponseProcessor;
import jakarta.inject.Singleton;

/**
 * Answers {@code 409} when a well-formed request cannot be applied to the register as it stands -
 * a badge another dog carries, a record kept only for audit, or one someone else has changed.
 */
@Singleton
@Produces(MediaType.APPLICATION_JSON)
public class ResourceConflictExceptionHandler
        implements ExceptionHandler<ResourceConflictException, HttpResponse<?>> {

    private final ErrorResponseProcessor<?> errorResponseProcessor;

    public ResourceConflictExceptionHandler(ErrorResponseProcessor<?> errorResponseProcessor) {
        this.errorResponseProcessor = errorResponseProcessor;
    }

    @Override
    public HttpResponse<?> handle(HttpRequest request, ResourceConflictException exception) {
        return errorResponseProcessor.processResponse(
                ErrorContext.builder(request).cause(exception).errorMessage(exception.getMessage()).build(),
                HttpResponse.status(HttpStatus.CONFLICT));
    }
}
