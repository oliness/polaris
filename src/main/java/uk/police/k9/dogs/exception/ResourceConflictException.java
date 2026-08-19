package uk.police.k9.dogs.exception;

/** A well-formed request that cannot be applied to the register. Surfaces as {@code 409}. */
public class ResourceConflictException extends RuntimeException {

    private ResourceConflictException(String message) {
        super(message);
    }

    /** A value that has to be unique among the active records is already taken. */
    public static ResourceConflictException duplicate(String resource, String field, Object value) {
        return new ResourceConflictException(
                "%s with %s '%s' already exists".formatted(resource, field, value));
    }

    /** The caller referred to a record that is only being kept for audit. */
    public static ResourceConflictException deleted(String resource, Object identifier) {
        return new ResourceConflictException(
                "%s %s has been deleted and is retained for audit only".formatted(resource, identifier));
    }
}
