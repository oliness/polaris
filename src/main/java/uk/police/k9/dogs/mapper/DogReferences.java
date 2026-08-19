package uk.police.k9.dogs.mapper;

import io.micronaut.core.annotation.Nullable;
import uk.police.k9.dogs.entity.DogStatus;
import uk.police.k9.dogs.entity.LeavingReason;
import uk.police.k9.dogs.entity.Supplier;

/**
 * The related records a dog points at, already loaded and checked by the service, so
 * {@link DogMapper} does not reach for a repository itself.
 */
public record DogReferences(Supplier supplier, DogStatus status, @Nullable LeavingReason leavingReason) {
}
