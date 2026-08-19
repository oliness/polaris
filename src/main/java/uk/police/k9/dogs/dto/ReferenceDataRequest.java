package uk.police.k9.dogs.dto;

import io.micronaut.core.annotation.Nullable;
import io.micronaut.serde.annotation.Serdeable;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/** The details needed to create or replace a dog status or a leaving reason; both share it. */
@Serdeable
@Schema(name = "ReferenceDataRequest", description = "A maintainable lookup value")
public record ReferenceDataRequest(

        @NotBlank
        @Size(max = 50)
        @Pattern(regexp = "^[A-Z][A-Z0-9_]*$",
                message = "must be upper case letters, digits and underscores, starting with a letter")
        @Schema(description = "Stable machine-readable identifier", example = "IN_SERVICE")
        String code,

        @NotBlank
        @Size(max = 100)
        @Schema(description = "Text shown to users", example = "In Service")
        String label,

        @Nullable
        @Size(max = 500)
        @Schema(example = "Operationally deployed with a handler.")
        String description,

        @Nullable
        @PositiveOrZero
        @Schema(description = "Lowest first when the values are listed. Defaults to 0.", example = "20")
        Integer displayOrder
) {
}
