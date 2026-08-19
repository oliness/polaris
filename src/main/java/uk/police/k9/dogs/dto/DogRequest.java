package uk.police.k9.dogs.dto;

import io.micronaut.core.annotation.Nullable;
import io.micronaut.serde.annotation.Serdeable;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import uk.police.k9.dogs.entity.Gender;
import uk.police.k9.dogs.validation.ValidDogTimeline;

import java.time.LocalDate;

/**
 * The details needed to create or replace a dog. Relationships are supplied as identifiers; the
 * rules spanning more than one field are enforced by {@link ValidDogTimeline}.
 */
@Serdeable
@ValidDogTimeline
@Schema(name = "DogRequest", description = "A dog registered with the force")
public record DogRequest(

        @NotBlank
        @Size(max = 100)
        @Schema(description = "The dog's call name", example = "Baxter")
        String name,

        @NotBlank
        @Size(max = 100)
        @Schema(example = "German Shepherd")
        String breed,

        @NotNull
        @Positive
        @Schema(description = "Identifier of the breeder or kennels the dog came from", example = "1")
        Long supplierId,

        @Nullable
        @Size(max = 30)
        @Schema(description = "Collar number. Absent until the dog is badged.", example = "K9-1041")
        String badgeId,

        @NotNull
        @Schema(description = "One of the codes returned by GET /api/dogs/genders", example = "MALE")
        Gender gender,

        @NotNull
        @Past
        @Schema(example = "2020-03-14")
        LocalDate birthDate,

        @NotNull
        @PastOrPresent
        @Schema(description = "The date the force took the dog on", example = "2021-01-06")
        LocalDate dateAcquired,

        @NotNull
        @Positive
        @Schema(description = "Identifier of a value from GET /api/dogs/statuses", example = "2")
        Long statusId,

        @Nullable
        @PastOrPresent
        @Schema(description = "Required if a leaving reason is given", example = "2025-03-31")
        LocalDate leavingDate,

        @Nullable
        @Positive
        @Schema(description = "Identifier of a value from GET /api/dogs/leaving-reasons. "
                + "Required if a leaving date is given.", example = "5")
        Long leavingReasonId,

        @Nullable
        @Size(max = 2000)
        @Schema(description = "Anything kennel staff need to know while the dog is with them",
                example = "Settles quickly. Must not be kennelled next to entire males.")
        String kennellingCharacteristic
) {
}
