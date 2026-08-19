package uk.police.k9.dogs.entity;

import io.micronaut.core.annotation.Nullable;
import io.micronaut.data.annotation.MappedEntity;
import io.micronaut.data.annotation.MappedProperty;
import io.micronaut.data.annotation.Relation;
import io.micronaut.data.annotation.TypeDef;
import io.micronaut.data.model.DataType;

import java.time.LocalDate;

@MappedEntity("dog")
public class Dog extends AuditedEntity {

    private String name;

    private String breed;

    @Relation(Relation.Kind.MANY_TO_ONE)
    @MappedProperty("supplier_id")
    private Supplier supplier;

    /** Absent until the dog is badged. */
    @Nullable
    private String badgeId;

    @TypeDef(type = DataType.STRING)
    private Gender gender;

    private LocalDate birthDate;

    private LocalDate dateAcquired;

    @Relation(Relation.Kind.MANY_TO_ONE)
    @MappedProperty("status_id")
    private DogStatus status;

    @Nullable
    private LocalDate leavingDate;

    @Relation(Relation.Kind.MANY_TO_ONE)
    @MappedProperty("leaving_reason_id")
    @Nullable
    private LeavingReason leavingReason;

    @Nullable
    private String kennellingCharacteristic;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBreed() {
        return breed;
    }

    public void setBreed(String breed) {
        this.breed = breed;
    }

    public Supplier getSupplier() {
        return supplier;
    }

    public void setSupplier(Supplier supplier) {
        this.supplier = supplier;
    }

    @Nullable
    public String getBadgeId() {
        return badgeId;
    }

    public void setBadgeId(@Nullable String badgeId) {
        this.badgeId = badgeId;
    }

    public Gender getGender() {
        return gender;
    }

    public void setGender(Gender gender) {
        this.gender = gender;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }

    public LocalDate getDateAcquired() {
        return dateAcquired;
    }

    public void setDateAcquired(LocalDate dateAcquired) {
        this.dateAcquired = dateAcquired;
    }

    public DogStatus getStatus() {
        return status;
    }

    public void setStatus(DogStatus status) {
        this.status = status;
    }

    @Nullable
    public LocalDate getLeavingDate() {
        return leavingDate;
    }

    public void setLeavingDate(@Nullable LocalDate leavingDate) {
        this.leavingDate = leavingDate;
    }

    @Nullable
    public LeavingReason getLeavingReason() {
        return leavingReason;
    }

    public void setLeavingReason(@Nullable LeavingReason leavingReason) {
        this.leavingReason = leavingReason;
    }

    @Nullable
    public String getKennellingCharacteristic() {
        return kennellingCharacteristic;
    }

    public void setKennellingCharacteristic(@Nullable String kennellingCharacteristic) {
        this.kennellingCharacteristic = kennellingCharacteristic;
    }
}
