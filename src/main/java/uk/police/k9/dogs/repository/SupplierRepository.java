package uk.police.k9.dogs.repository;

import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.model.query.builder.sql.Dialect;
import io.micronaut.data.repository.PageableRepository;
import io.micronaut.data.repository.jpa.JpaSpecificationExecutor;
import uk.police.k9.dogs.entity.Supplier;

import java.util.Optional;

/**
 * Data access for {@link Supplier}. {@code JpaSpecificationExecutor} lets the list endpoint
 * combine "not deleted" with the caller's search terms without a query per combination.
 */
@JdbcRepository(dialect = Dialect.H2)
public interface SupplierRepository extends PageableRepository<Supplier, Long>,
        JpaSpecificationExecutor<Supplier> {

    Optional<Supplier> findByNameIgnoreCaseAndDeletedAtIsNull(String name);
}
