package uk.police.k9.dogs.exception;

/**
 * Thrown when a caller's {@code If-Match} names a version the record has already moved on from.
 * Surfaces as {@code 412 Precondition Failed}.
 */
public class PreconditionFailedException extends RuntimeException {

    private PreconditionFailedException(String message) {
        super(message);
    }

    public static PreconditionFailedException staleVersion(String resource, Object identifier) {
        return new PreconditionFailedException(
                "%s %s has changed since you read it. Fetch it again and re-apply the change."
                        .formatted(resource, identifier));
    }
}
