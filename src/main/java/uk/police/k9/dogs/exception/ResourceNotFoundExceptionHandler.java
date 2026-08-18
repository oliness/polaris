package uk.police.k9.dogs.exception;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Produces;
import io.micronaut.http.server.exceptions.ExceptionHandler;
import io.micronaut.http.server.exceptions.response.ErrorContext;
import io.micronaut.http.server.exceptions.response.ErrorResponseProcessor;
import jakarta.inject.Singleton;

/**
 * Answers {@code 404} when a request names a record that does not exist. Handling it here leaves
 * the services free to speak in domain terms and keeps the mapping to HTTP in one place.
 */
@Singleton
@Produces(MediaType.APPLICATION_JSON)
public class ResourceNotFoundExceptionHandler
        implements ExceptionHandler<ResourceNotFoundException, HttpResponse<?>> {

    private final ErrorResponseProcessor<?> errorResponseProcessor;

    public ResourceNotFoundExceptionHandler(ErrorResponseProcessor<?> errorResponseProcessor) {
        this.errorResponseProcessor = errorResponseProcessor;
    }

    @Override
    public HttpResponse<?> handle(HttpRequest request, ResourceNotFoundException exception) {
        return errorResponseProcessor.processResponse(
                ErrorContext.builder(request).cause(exception).errorMessage(exception.getMessage()).build(),
                HttpResponse.notFound());
    }
}
