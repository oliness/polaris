package uk.police.k9.dogs.exception;

import io.micronaut.data.exceptions.OptimisticLockException;
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
 * Answers {@code 409} when two transactions interleaved and the second write to reach the database
 * was refused by the {@code version} column. A caller who sends the version it read is refused
 * earlier, by the service; this covers the race that gets past that.
 */
@Singleton
@Produces(MediaType.APPLICATION_JSON)
public class OptimisticLockExceptionHandler
        implements ExceptionHandler<OptimisticLockException, HttpResponse<?>> {

    private static final String MESSAGE =
            "The record was changed by someone else. Fetch it again and re-apply the change.";

    private final ErrorResponseProcessor<?> errorResponseProcessor;

    public OptimisticLockExceptionHandler(ErrorResponseProcessor<?> errorResponseProcessor) {
        this.errorResponseProcessor = errorResponseProcessor;
    }

    @Override
    public HttpResponse<?> handle(HttpRequest request, OptimisticLockException exception) {
        return errorResponseProcessor.processResponse(
                ErrorContext.builder(request).cause(exception).errorMessage(MESSAGE).build(),
                HttpResponse.status(HttpStatus.CONFLICT));
    }
}
