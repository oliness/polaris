package uk.police.k9.dogs.validation;

import io.micronaut.context.annotation.Factory;
import io.micronaut.validation.validator.constraints.ConstraintValidator;
import jakarta.inject.Singleton;
import uk.police.k9.dogs.dto.DogRequest;

/**
 * Supplies the validator behind {@link ValidDogTimeline}. Micronaut resolves constraint validators
 * as beans, and each failure sets its own message so the caller is told which rule was broken.
 */
@Factory
public class DogValidationFactory {

    @Singleton
    ConstraintValidator<ValidDogTimeline, DogRequest> dogTimelineValidator() {
        return (dog, annotationMetadata, context) -> {
            if (dog == null) {
                return true;
            }

            if (dog.birthDate() != null && dog.dateAcquired() != null
                    && dog.dateAcquired().isBefore(dog.birthDate())) {
                context.messageTemplate("dateAcquired must not be before birthDate");
                return false;
            }

            if (dog.leavingDate() != null && dog.dateAcquired() != null
                    && dog.leavingDate().isBefore(dog.dateAcquired())) {
                context.messageTemplate("leavingDate must not be before dateAcquired");
                return false;
            }

            if (dog.leavingDate() != null && dog.leavingReasonId() == null) {
                context.messageTemplate("leavingReasonId is required when leavingDate is set");
                return false;
            }

            if (dog.leavingReasonId() != null && dog.leavingDate() == null) {
                context.messageTemplate("leavingDate is required when leavingReasonId is set");
                return false;
            }

            return true;
        };
    }
}
