package uk.police.k9.dogs.support;

import uk.police.k9.dogs.dto.DogRequest;
import uk.police.k9.dogs.entity.Gender;

import java.time.LocalDate;

/**
 * Builds a valid {@link DogRequest} so each test states only the part it cares about - a dog has
 * enough required fields to bury the value under test otherwise.
 */
public final class DogRequestBuilder {

    private String name = "Baxter";
    private String breed = "German Shepherd";
    private Long supplierId;
    private String badgeId;
    private Gender gender = Gender.MALE;
    private LocalDate birthDate = LocalDate.of(2020, 3, 14);
    private LocalDate dateAcquired = LocalDate.of(2021, 1, 6);
    private Long statusId;
    private LocalDate leavingDate;
    private Long leavingReasonId;
    private String kennellingCharacteristic;

    private DogRequestBuilder(Long supplierId, Long statusId) {
        this.supplierId = supplierId;
        this.statusId = statusId;
    }

    public static DogRequestBuilder aDog(Long supplierId, Long statusId) {
        return new DogRequestBuilder(supplierId, statusId);
    }

    public DogRequestBuilder name(String name) {
        this.name = name;
        return this;
    }

    public DogRequestBuilder breed(String breed) {
        this.breed = breed;
        return this;
    }

    public DogRequestBuilder supplierId(Long supplierId) {
        this.supplierId = supplierId;
        return this;
    }

    public DogRequestBuilder badgeId(String badgeId) {
        this.badgeId = badgeId;
        return this;
    }

    public DogRequestBuilder gender(Gender gender) {
        this.gender = gender;
        return this;
    }

    public DogRequestBuilder birthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
        return this;
    }

    public DogRequestBuilder dateAcquired(LocalDate dateAcquired) {
        this.dateAcquired = dateAcquired;
        return this;
    }

    public DogRequestBuilder statusId(Long statusId) {
        this.statusId = statusId;
        return this;
    }

    /** The two halves of a departure travel together, so they are set together. */
    public DogRequestBuilder left(LocalDate leavingDate, Long leavingReasonId) {
        this.leavingDate = leavingDate;
        this.leavingReasonId = leavingReasonId;
        return this;
    }

    public DogRequestBuilder leavingDate(LocalDate leavingDate) {
        this.leavingDate = leavingDate;
        return this;
    }

    public DogRequestBuilder leavingReasonId(Long leavingReasonId) {
        this.leavingReasonId = leavingReasonId;
        return this;
    }

    public DogRequestBuilder kennellingCharacteristic(String kennellingCharacteristic) {
        this.kennellingCharacteristic = kennellingCharacteristic;
        return this;
    }

    public DogRequest build() {
        return new DogRequest(name, breed, supplierId, badgeId, gender, birthDate, dateAcquired,
                statusId, leavingDate, leavingReasonId, kennellingCharacteristic);
    }
}
