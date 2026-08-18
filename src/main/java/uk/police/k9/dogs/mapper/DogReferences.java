package uk.police.k9.dogs.mapper;

import io.micronaut.core.annotation.Nullable;
import uk.police.k9.dogs.entity.DogStatus;
import uk.police.k9.dogs.entity.LeavingReason;
import uk.police.k9.dogs.entity.Supplier;

/**
 * The related records a dog points at, already loaded and checked. Resolving an identifier is the
 * service's job, so the results are handed to {@link DogMapper} rather than the mapper reaching
 * for a repository itself.
 */
public record DogReferences(Supplier supplier, DogStatus status, @Nullable LeavingReason leavingReason) {
}
