package uk.police.k9.dogs.exception;

import io.micronaut.context.annotation.Replaces;
import io.micronaut.core.convert.exceptions.ConversionErrorException;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Produces;
import io.micronaut.http.server.exceptions.ConversionErrorHandler;
import io.micronaut.http.server.exceptions.ExceptionHandler;
import io.micronaut.http.server.exceptions.response.ErrorContext;
import io.micronaut.http.server.exceptions.response.ErrorResponseProcessor;
import jakarta.inject.Singleton;

/**
 * Answers {@code 400} when a parameter could not be converted - most often a {@code filter} that
 * is not the JSON the dogs list expects. Micronaut's own handler buries the useful part in
 * {@code Failed to convert argument [filter] for value [...]}; this one names the parameter in
 * {@code details} and lets the underlying explanation be the message.
 */
@Singleton
@Replaces(ConversionErrorHandler.class)
@Produces(MediaType.APPLICATION_JSON)
public class ConversionExceptionHandler
        implements ExceptionHandler<ConversionErrorException, HttpResponse<?>> {

    private static final String CONVERSION_FAILED = "The request failed validation";

    private final ErrorResponseProcessor<?> errorResponseProcessor;

    public ConversionExceptionHandler(ErrorResponseProcessor<?> errorResponseProcessor) {
        this.errorResponseProcessor = errorResponseProcessor;
    }

    @Override
    public HttpResponse<?> handle(HttpRequest request, ConversionErrorException exception) {
        ErrorContext context = ErrorContext.builder(request)
                .cause(exception)
                .errorMessage(CONVERSION_FAILED)
                .error(new RequestError(exception.getArgument().getName(), reason(exception)))
                .build();
        return errorResponseProcessor.processResponse(context, HttpResponse.badRequest());
    }

    /** Prefers the message from whatever refused the value over the framework's wrapper around it. */
    private static String reason(ConversionErrorException exception) {
        Exception cause = exception.getConversionError().getCause();
        if (cause != null && cause.getMessage() != null && !cause.getMessage().isBlank()) {
            return cause.getMessage();
        }
        return exception.getMessage();
    }
}
