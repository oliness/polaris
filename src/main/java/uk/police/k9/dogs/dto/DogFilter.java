package uk.police.k9.dogs.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.core.util.StringUtils;
import io.micronaut.serde.annotation.Serdeable;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * The search terms accepted by the dogs list endpoint, supplied as JSON in the {@code filter}
 * query parameter. Each term is an optional, case-insensitive "contains" match combined with AND;
 * an unknown key is rejected rather than ignored, so a mistyped term fails loudly.
 */
@Serdeable
@JsonIgnoreProperties(ignoreUnknown = false)
@Schema(name = "DogFilter", description = "Search terms for the dogs list endpoint")
public record DogFilter(

        @Nullable
        @Schema(description = "Matches part of the dog's name", example = "bax")
        String name,

        @Nullable
        @Schema(description = "Matches part of the breed", example = "malinois")
        String breed,

        @Nullable
        @Schema(description = "Matches part of the supplier's name", example = "ashcombe")
        String supplier
) {

    private static final DogFilter EMPTY = new DogFilter(null, null, null);

    public static DogFilter empty() {
        return EMPTY;
    }

    public boolean hasName() {
        return StringUtils.isNotEmpty(trimmed(name));
    }

    public boolean hasBreed() {
        return StringUtils.isNotEmpty(trimmed(breed));
    }

    public boolean hasSupplier() {
        return StringUtils.isNotEmpty(trimmed(supplier));
    }

    @Nullable
    private static String trimmed(@Nullable String value) {
        return value == null ? null : value.trim();
    }
}
