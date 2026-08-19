package uk.police.k9.dogs.entity;

import io.micronaut.core.annotation.Nullable;
import io.micronaut.data.annotation.MappedEntity;

/**
 * The breeder or kennels a dog came from. Its own table because more than one dog can come from
 * the same supplier.
 */
@MappedEntity("supplier")
public class Supplier extends AuditedEntity {

    private String name;

    @Nullable
    private String contactName;

    @Nullable
    private String contactEmail;

    @Nullable
    private String contactPhone;

    @Nullable
    private String address;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Nullable
    public String getContactName() {
        return contactName;
    }

    public void setContactName(@Nullable String contactName) {
        this.contactName = contactName;
    }

    @Nullable
    public String getContactEmail() {
        return contactEmail;
    }

    public void setContactEmail(@Nullable String contactEmail) {
        this.contactEmail = contactEmail;
    }

    @Nullable
    public String getContactPhone() {
        return contactPhone;
    }

    public void setContactPhone(@Nullable String contactPhone) {
        this.contactPhone = contactPhone;
    }

    @Nullable
    public String getAddress() {
        return address;
    }

    public void setAddress(@Nullable String address) {
        this.address = address;
    }
}
