package uk.police.k9.dogs.entity;

import io.micronaut.data.annotation.MappedEntity;

/** A status a dog can hold. Maintained through {@code /api/dogs/statuses}. */
@MappedEntity("dog_status")
public class DogStatus extends ReferenceDataEntity {
}
