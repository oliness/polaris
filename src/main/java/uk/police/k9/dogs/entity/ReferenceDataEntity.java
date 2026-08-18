package uk.police.k9.dogs.entity;

import io.micronaut.core.annotation.Nullable;

/**
 * Base class for the lookup tables the force maintains through the API - the statuses and leaving
 * reasons the task describes as the values that are <em>currently</em> possible.
 */
public abstract class ReferenceDataEntity extends AuditedEntity {

    /** Stable machine-readable identifier, e.g. {@code IN_SERVICE}. */
    private String code;

    /** Text shown to users, e.g. {@code In Service}. */
    private String label;

    @Nullable
    private String description;

    /** Lowest first when the values are listed. */
    private int displayOrder;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    @Nullable
    public String getDescription() {
        return description;
    }

    public void setDescription(@Nullable String description) {
        this.description = description;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(int displayOrder) {
        this.displayOrder = displayOrder;
    }
}
