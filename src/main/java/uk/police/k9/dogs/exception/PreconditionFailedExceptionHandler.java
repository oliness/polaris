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

/** Answers {@code 412} when the record no longer carries the version {@code If-Match} asked for. */
@Singleton
@Produces(MediaType.APPLICATION_JSON)
public class PreconditionFailedExceptionHandler
        implements ExceptionHandler<PreconditionFailedException, HttpResponse<?>> {

    private final ErrorResponseProcessor<?> errorResponseProcessor;

    public PreconditionFailedExceptionHandler(ErrorResponseProcessor<?> errorResponseProcessor) {
        this.errorResponseProcessor = errorResponseProcessor;
    }

    @Override
    public HttpResponse<?> handle(HttpRequest request, PreconditionFailedException exception) {
        return errorResponseProcessor.processResponse(
                ErrorContext.builder(request).cause(exception).errorMessage(exception.getMessage()).build(),
                HttpResponse.status(HttpStatus.PRECONDITION_FAILED));
    }
}
