package uk.police.k9.dogs.dto;

import io.micronaut.core.annotation.Nullable;
import io.micronaut.serde.annotation.Serdeable;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/** A supplier as returned by the API. */
@Serdeable
@Schema(name = "SupplierResponse", description = "A breeder or kennels that supplies dogs")
public record SupplierResponse(
        Long id,
        String name,
        @Nullable String contactName,
        @Nullable String contactEmail,
        @Nullable String contactPhone,
        @Nullable String address,
        @Schema(description = "True when the supplier has been deleted and is retained for audit only")
        boolean deleted,
        @Nullable Instant deletedAt,
        Instant createdAt,
        Instant updatedAt,
        @Schema(description = "Incremented on every update; used for optimistic locking")
        Long version
) {
}
