package uk.police.k9.dogs.repository;

import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.model.query.builder.sql.Dialect;
import uk.police.k9.dogs.entity.DogStatus;

/** Data access for the statuses a dog can hold. */
@JdbcRepository(dialect = Dialect.H2)
public interface DogStatusRepository extends ReferenceDataRepository<DogStatus> {
}
