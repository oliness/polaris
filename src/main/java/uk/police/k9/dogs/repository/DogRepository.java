package uk.police.k9.dogs.repository;

import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.data.annotation.Join;
import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.data.model.query.builder.sql.Dialect;
import io.micronaut.data.repository.PageableRepository;
import io.micronaut.data.repository.jpa.JpaSpecificationExecutor;
import io.micronaut.data.repository.jpa.criteria.PredicateSpecification;
import uk.police.k9.dogs.entity.Dog;

import java.util.Optional;

/**
 * Data access for {@link Dog}. The {@code @Join} declarations fetch the related rows in the same
 * statement; without them the list endpoint issues three extra queries per row.
 */
@JdbcRepository(dialect = Dialect.H2)
public interface DogRepository extends PageableRepository<Dog, Long>, JpaSpecificationExecutor<Dog> {

    @Join(value = "supplier", type = Join.Type.LEFT_FETCH)
    @Join(value = "status", type = Join.Type.LEFT_FETCH)
    @Join(value = "leavingReason", type = Join.Type.LEFT_FETCH)
    @Override
    @NonNull
    Optional<Dog> findById(@NonNull Long id);

    @Join(value = "supplier", type = Join.Type.LEFT_FETCH)
    @Join(value = "status", type = Join.Type.LEFT_FETCH)
    @Join(value = "leavingReason", type = Join.Type.LEFT_FETCH)
    @Override
    Page<Dog> findAll(@Nullable PredicateSpecification<Dog> spec, Pageable pageable);

    Optional<Dog> findByBadgeIdIgnoreCaseAndDeletedAtIsNull(String badgeId);
}
