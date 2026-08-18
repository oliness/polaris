package uk.police.k9.dogs.entity;

import io.micronaut.core.annotation.Nullable;
import io.micronaut.data.annotation.DateCreated;
import io.micronaut.data.annotation.DateUpdated;
import io.micronaut.data.annotation.GeneratedValue;
import io.micronaut.data.annotation.Id;
import io.micronaut.data.annotation.Transient;
import io.micronaut.data.annotation.Version;

import java.time.Instant;

/**
 * Fields shared by every table in the register. Nothing is ever physically removed:
 * {@link #deletedAt} is stamped instead, so the audit trail survives.
 */
public abstract class AuditedEntity {

    @Id
    @GeneratedValue(GeneratedValue.Type.IDENTITY)
    private Long id;

    @Version
    private Long version;

    @DateCreated
    private Instant createdAt;

    @DateUpdated
    private Instant updatedAt;

    @Nullable
    private Instant deletedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Nullable
    public Instant getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(@Nullable Instant deletedAt) {
        this.deletedAt = deletedAt;
    }

    @Transient
    public boolean isDeleted() {
        return deletedAt != null;
    }
}
