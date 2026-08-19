package uk.police.k9.dogs.service;

import io.micronaut.core.annotation.Nullable;
import uk.police.k9.dogs.entity.AuditedEntity;
import uk.police.k9.dogs.exception.PreconditionFailedException;
import uk.police.k9.dogs.exception.ResourceConflictException;

import java.util.Set;

/** The checks every update makes before it is allowed to touch a record. */
final class Updates {

    private Updates() {
    }

    /**
     * Refuses a record kept only for audit, and one that has moved on since the caller read it.
     *
     * @param expectedVersions the versions {@code If-Match} will accept, or {@code null} when no
     *                         precondition was set - the update then works without protection
     */
    static void requireEditable(AuditedEntity entity, @Nullable Set<Long> expectedVersions,
                                String resource) {
        if (entity.isDeleted()) {
            throw ResourceConflictException.deleted(resource, entity.getId());
        }
        if (expectedVersions != null && !expectedVersions.contains(entity.getVersion())) {
            throw PreconditionFailedException.staleVersion(resource, entity.getId());
        }
    }
}
