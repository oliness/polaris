package uk.police.k9.dogs.repository;

import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.model.query.builder.sql.Dialect;
import uk.police.k9.dogs.entity.LeavingReason;

/** Data access for the reasons a dog can leave the force. */
@JdbcRepository(dialect = Dialect.H2)
public interface LeavingReasonRepository extends ReferenceDataRepository<LeavingReason> {
}
