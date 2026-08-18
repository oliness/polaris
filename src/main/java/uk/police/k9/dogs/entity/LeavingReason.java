package uk.police.k9.dogs.entity;

import io.micronaut.data.annotation.MappedEntity;

/** A reason a dog left the force. Maintained through {@code /api/dogs/leaving-reasons}. */
@MappedEntity("leaving_reason")
public class LeavingReason extends ReferenceDataEntity {
}
