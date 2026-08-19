package uk.police.k9.dogs.repository;

import io.micronaut.data.repository.PageableRepository;
import io.micronaut.data.repository.jpa.JpaSpecificationExecutor;
import uk.police.k9.dogs.entity.ReferenceDataEntity;

import java.util.Optional;

/**
 * The data access every reference-data table needs. Micronaut Data resolves {@code E} when it
 * generates each implementation, so the subinterfaces stay empty.
 */
public interface ReferenceDataRepository<E extends ReferenceDataEntity>
        extends PageableRepository<E, Long>, JpaSpecificationExecutor<E> {

    Optional<E> findByCodeIgnoreCaseAndDeletedAtIsNull(String code);
}
