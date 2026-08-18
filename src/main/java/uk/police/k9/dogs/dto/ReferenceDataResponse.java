package uk.police.k9.dogs.dto;

import io.micronaut.core.annotation.Nullable;
import io.micronaut.serde.annotation.Serdeable;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/** A dog status or a leaving reason, as returned by the API. */
@Serdeable
@Schema(name = "ReferenceDataResponse", description = "A maintainable lookup value")
public record ReferenceDataResponse(
        Long id,
        @Schema(example = "IN_SERVICE") String code,
        @Schema(example = "In Service") String label,
        @Nullable String description,
        int displayOrder,
        @Schema(description = "True when the value has been deleted and is retained for audit only")
        boolean deleted,
        @Nullable Instant deletedAt,
        Instant createdAt,
        Instant updatedAt,
        @Schema(description = "Incremented on every update; used for optimistic locking")
        Long version
) {
}
