package uk.police.k9.dogs.exception;

/** Thrown when a request names a record that does not exist. Surfaces as {@code 404}. */
public class ResourceNotFoundException extends RuntimeException {

    private ResourceNotFoundException(String message) {
        super(message);
    }

    public static ResourceNotFoundException of(String resource, Object identifier) {
        return new ResourceNotFoundException("%s %s does not exist".formatted(resource, identifier));
    }
}
