package uk.police.k9.dogs.dto;

import io.micronaut.core.annotation.Nullable;
import io.micronaut.serde.annotation.Serdeable;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** The details needed to create or replace a supplier. */
@Serdeable
@Schema(name = "SupplierRequest", description = "A breeder or kennels that supplies dogs")
public record SupplierRequest(

        @NotBlank
        @Size(max = 150)
        @Schema(description = "Trading name of the breeder or kennels", example = "Ravenscroft Working Dogs")
        String name,

        @Nullable
        @Size(max = 150)
        @Schema(description = "Named contact at the supplier", example = "Marie Ravenscroft")
        String contactName,

        @Nullable
        @Email
        @Size(max = 255)
        @Schema(example = "kennels@ravenscroft.example")
        String contactEmail,

        @Nullable
        @Size(max = 30)
        @Schema(example = "01592 555 210")
        String contactPhone,

        @Nullable
        @Size(max = 500)
        @Schema(example = "Ravenscroft Farm, Kinross, KY13 9XX")
        String address
) {
}
