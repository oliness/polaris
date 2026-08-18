package uk.police.k9.dogs.validation;

import jakarta.validation.Constraint;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Class-level constraint on the rules spanning more than one field of a dog: it cannot be acquired
 * before it was born or leave before it was acquired, and a leaving date and reason travel
 * together. Implemented by {@link DogValidationFactory}.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Constraint(validatedBy = {})
public @interface ValidDogTimeline {

    String message() default "the dog's dates are inconsistent";
}
