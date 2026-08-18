package uk.police.k9.dogs.dto;

import io.micronaut.serde.annotation.Serdeable;
import io.swagger.v3.oas.annotations.media.Schema;
import uk.police.k9.dogs.entity.Gender;

/**
 * One of the permitted values for a dog's gender, in the same {@code code}/{@code label} shape as
 * the maintainable lookups so a client can render every enumerated field the same way.
 */
@Serdeable
@Schema(name = "GenderResponse", description = "A permitted value for a dog's gender")
public record GenderResponse(
        @Schema(example = "MALE") String code,
        @Schema(example = "Male") String label
) {

    public static GenderResponse from(Gender gender) {
        return new GenderResponse(gender.name(), gender.getLabel());
    }
}
