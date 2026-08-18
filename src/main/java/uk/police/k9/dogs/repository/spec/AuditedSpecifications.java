package uk.police.k9.dogs.repository.spec;

import io.micronaut.data.repository.jpa.criteria.PredicateSpecification;
import uk.police.k9.dogs.entity.AuditedEntity;

/** Criteria shared by every table, all of which carry the audit columns. */
public final class AuditedSpecifications {

    /** Property name on {@link AuditedEntity}. */
    static final String DELETED_AT = "deletedAt";

    private AuditedSpecifications() {
    }

    /** Keeps deleted records out of the list endpoints while leaving them in the database. */
    public static <T extends AuditedEntity> PredicateSpecification<T> notDeleted() {
        return (root, criteriaBuilder) -> criteriaBuilder.isNull(root.get(DELETED_AT));
    }
}
