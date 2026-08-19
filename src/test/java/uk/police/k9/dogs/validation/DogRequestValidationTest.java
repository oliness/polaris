package uk.police.k9.dogs.validation;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.micronaut.validation.validator.Validator;
import jakarta.inject.Inject;
import jakarta.validation.ConstraintViolation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.police.k9.dogs.dto.DogRequest;
import uk.police.k9.dogs.support.DogRequestBuilder;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The constraints on a dog, checked against the validator rather than through HTTP so each rule
 * can be stated on its own.
 */
@MicronautTest(startApplication = false)
@DisplayName("The details supplied for a dog")
class DogRequestValidationTest {

    private static final long SUPPLIER_ID = 1L;
    private static final long STATUS_ID = 2L;
    private static final long LEAVING_REASON_ID = 3L;

    @Inject
    Validator validator;

    @Test
    @DisplayName("are accepted when they are complete and consistent")
    void acceptsAValidDog() {
        assertThat(validate(DogRequestBuilder.aDog(SUPPLIER_ID, STATUS_ID).build())).isEmpty();
    }

    @Test
    @DisplayName("must include the fields the register cannot do without")
    void requiresTheMandatoryFields() {
        DogRequest request = new DogRequest(" ", "", null, null, null, null, null, null, null, null, null);

        assertThat(messagesByField(request))
                .containsKeys("name", "breed", "supplierId", "gender", "birthDate", "dateAcquired", "statusId");
    }

    @Test
    @DisplayName("may leave out the details a dog has not got yet")
    void allowsTheOptionalFields() {
        DogRequest request = DogRequestBuilder.aDog(SUPPLIER_ID, STATUS_ID)
                .badgeId(null)
                .kennellingCharacteristic(null)
                .build();

        assertThat(validate(request)).isEmpty();
    }

    @Test
    @DisplayName("cannot claim the dog was born in the future")
    void rejectsAFutureBirthDate() {
        DogRequest request = DogRequestBuilder.aDog(SUPPLIER_ID, STATUS_ID)
                .birthDate(LocalDate.now().plusDays(1))
                .build();

        assertThat(messagesByField(request)).containsKey("birthDate");
    }

    @Test
    @DisplayName("cannot have the dog arriving before it was born")
    void rejectsAcquisitionBeforeBirth() {
        DogRequest request = DogRequestBuilder.aDog(SUPPLIER_ID, STATUS_ID)
                .birthDate(LocalDate.of(2021, 1, 1))
                .dateAcquired(LocalDate.of(2020, 1, 1))
                .build();

        assertThat(messages(request)).contains("dateAcquired must not be before birthDate");
    }

    @Test
    @DisplayName("cannot have the dog leaving before the force took it on")
    void rejectsLeavingBeforeAcquisition() {
        DogRequest request = DogRequestBuilder.aDog(SUPPLIER_ID, STATUS_ID)
                .dateAcquired(LocalDate.of(2021, 1, 6))
                .left(LocalDate.of(2020, 12, 31), LEAVING_REASON_ID)
                .build();

        assertThat(messages(request)).contains("leavingDate must not be before dateAcquired");
    }

    @Test
    @DisplayName("must say why a dog left, not only when")
    void requiresAReasonWithALeavingDate() {
        DogRequest request = DogRequestBuilder.aDog(SUPPLIER_ID, STATUS_ID)
                .leavingDate(LocalDate.of(2025, 3, 31))
                .build();

        assertThat(messages(request)).contains("leavingReasonId is required when leavingDate is set");
    }

    @Test
    @DisplayName("must say when a dog left, not only why")
    void requiresALeavingDateWithAReason() {
        DogRequest request = DogRequestBuilder.aDog(SUPPLIER_ID, STATUS_ID)
                .leavingReasonId(LEAVING_REASON_ID)
                .build();

        assertThat(messages(request)).contains("leavingDate is required when leavingReasonId is set");
    }

    private Set<ConstraintViolation<DogRequest>> validate(DogRequest request) {
        return validator.validate(request);
    }

    private List<String> messages(DogRequest request) {
        return validate(request).stream().map(ConstraintViolation::getMessage).toList();
    }

    private Map<String, String> messagesByField(DogRequest request) {
        return validate(request).stream().collect(Collectors.toMap(
                violation -> violation.getPropertyPath().toString(),
                ConstraintViolation::getMessage,
                (first, second) -> first));
    }
}
