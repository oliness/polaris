package uk.police.k9.dogs.dto;

import io.micronaut.core.annotation.Nullable;
import io.micronaut.serde.annotation.Serdeable;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.time.LocalDate;

/**
 * A dog as returned by the API, with relationships expanded so a client can render a list without
 * a second call per row.
 */
@Serdeable
@Schema(name = "DogResponse", description = "A dog registered with the force")
public record DogResponse(
        Long id,
        String name,
        String breed,
        SupplierResponse supplier,
        @Nullable String badgeId,
        GenderResponse gender,
        LocalDate birthDate,
        LocalDate dateAcquired,
        ReferenceDataResponse status,
        @Nullable LocalDate leavingDate,
        @Nullable ReferenceDataResponse leavingReason,
        @Nullable String kennellingCharacteristic,
        @Schema(description = "True when the dog has been deleted and is retained for audit only")
        boolean deleted,
        @Nullable Instant deletedAt,
        Instant createdAt,
        Instant updatedAt,
        @Schema(description = "Incremented on every update; used for optimistic locking")
        Long version
) {
}
