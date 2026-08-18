package uk.police.k9.dogs.controller;

import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Produces;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import uk.police.k9.dogs.dto.GenderResponse;
import uk.police.k9.dogs.entity.Gender;

import java.util.Arrays;
import java.util.List;

/**
 * The permitted values for a dog's gender. Gender is the one enumerated field on a dog that is a
 * closed set rather than something the force maintains, so it stays an enum and this endpoint is
 * read-only - but a client can still populate a drop-down from it as it does for the lookups.
 *
 * <p>The list is short, fixed and complete, so it is returned whole rather than paged.
 */
@Controller(ApiPaths.GENDERS)
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Genders")
public class GenderController {

    private static final List<GenderResponse> GENDERS =
            Arrays.stream(Gender.values()).map(GenderResponse::from).toList();

    @Get
    @Operation(summary = "List the permitted genders",
            description = "A fixed set, returned in full. Use the code when creating or updating a dog.")
    @ApiResponse(responseCode = "200", description = "Every permitted gender")
    public List<GenderResponse> list() {
        return GENDERS;
    }
}
