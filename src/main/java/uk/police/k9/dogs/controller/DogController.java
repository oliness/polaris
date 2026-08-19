package uk.police.k9.dogs.controller;

import io.micronaut.core.annotation.Nullable;
import io.micronaut.data.model.Pageable;
import io.micronaut.http.HttpHeaders;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Consumes;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Delete;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.Produces;
import io.micronaut.http.annotation.Put;
import io.micronaut.http.annotation.QueryValue;
import io.micronaut.http.annotation.Status;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.micronaut.validation.Validated;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import uk.police.k9.dogs.dto.DogFilter;
import uk.police.k9.dogs.dto.DogRequest;
import uk.police.k9.dogs.dto.DogResponse;
import uk.police.k9.dogs.dto.PagedResponse;
import uk.police.k9.dogs.service.DogService;

import java.net.URI;

/**
 * The dogs registered with the force: bind, validate, delegate to {@link DogService}, respond.
 * Database work is blocking, so the routes are dispatched onto the blocking executor.
 */
@Controller(ApiPaths.DOGS)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@ExecuteOn(TaskExecutors.BLOCKING)
@Validated
@Tag(name = "Dogs")
public class DogController {

    private final DogService dogService;

    public DogController(DogService dogService) {
        this.dogService = dogService;
    }

    @Get
    @Operation(summary = "List dogs",
            description = "Deleted dogs are excluded unless includeDeleted is true. "
                    + "The filter parameter takes JSON and accepts the keys name, breed and supplier, "
                    + "each matched case-insensitively anywhere in the value.",
            parameters = {
                    @Parameter(name = "filter", in = ParameterIn.QUERY,
                            description = "Search terms as JSON, with any of the keys name, breed "
                                    + "and supplier. Terms are combined with AND. A key that is not "
                                    + "one of the three is rejected.",
                            example = "{\"breed\":\"malinois\"}",
                            schema = @Schema(implementation = String.class)),
                    @Parameter(name = "page", in = ParameterIn.QUERY, description = ApiParameters.PAGE,
                            schema = @Schema(implementation = Integer.class, defaultValue = "0")),
                    @Parameter(name = "size", in = ParameterIn.QUERY, description = ApiParameters.SIZE,
                            schema = @Schema(implementation = Integer.class, defaultValue = "20")),
                    @Parameter(name = "sort", in = ParameterIn.QUERY, description = ApiParameters.SORT,
                            example = "name,desc",
                            schema = @Schema(implementation = String.class))})
    @ApiResponse(responseCode = "200", description = "A page of dogs")
    @ApiResponse(responseCode = "400", description = "The filter or paging parameters were invalid")
    public PagedResponse<DogResponse> list(
            // Documented by the operation above: left to itself the generator expands the record
            // into one query parameter per field and never mentions filter.
            @Parameter(hidden = true)
            @QueryValue @Nullable DogFilter filter,
            @Parameter(description = "Include dogs kept only for audit")
            @QueryValue(defaultValue = "false") boolean includeDeleted,
            @Parameter(hidden = true) Pageable pageable) {
        return dogService.list(filter == null ? DogFilter.empty() : filter, includeDeleted, pageable);
    }

    @Get("/{id}")
    @Operation(summary = "Get a dog")
    @ApiResponse(responseCode = "200", description = "The dog",
            headers = @Header(name = "ETag", description = ApiParameters.ETAG,
                    schema = @Schema(implementation = String.class)))
    @ApiResponse(responseCode = "404", description = "No dog has this identifier")
    public HttpResponse<DogResponse> get(@PathVariable Long id) {
        DogResponse dog = dogService.get(id);
        return ETags.ok(dog, dog.version());
    }

    @Post
    @Operation(summary = "Register a dog")
    @ApiResponse(responseCode = "201", description = "The dog was registered",
            headers = @Header(name = "ETag", description = ApiParameters.ETAG,
                    schema = @Schema(implementation = String.class)))
    @ApiResponse(responseCode = "400", description = "The request failed validation")
    @ApiResponse(responseCode = "404", description = "The supplier, status or leaving reason is unknown")
    @ApiResponse(responseCode = "409",
            description = "The badge is already in use, or the supplier, status or leaving reason "
                    + "has been deleted")
    public HttpResponse<DogResponse> create(@Valid @Body DogRequest request) {
        DogResponse created = dogService.create(request);
        return ETags.created(created, URI.create(ApiPaths.DOGS + "/" + created.id()), created.version());
    }

    @Put("/{id}")
    @Operation(summary = "Replace a dog")
    @ApiResponse(responseCode = "200", description = "The updated dog",
            headers = @Header(name = "ETag", description = ApiParameters.ETAG,
                    schema = @Schema(implementation = String.class)))
    @ApiResponse(responseCode = "400", description = "The request failed validation")
    @ApiResponse(responseCode = "404", description = "No dog has this identifier")
    @ApiResponse(responseCode = "409",
            description = "The dog or something it refers to is deleted, or the badge is in use")
    @ApiResponse(responseCode = "412", description = "The dog has changed since If-Match was read")
    public HttpResponse<DogResponse> update(
            @PathVariable Long id,
            @Valid @Body DogRequest request,
            @Parameter(description = ApiParameters.IF_MATCH)
            @io.micronaut.http.annotation.Header(HttpHeaders.IF_MATCH) @Nullable String ifMatch) {
        DogResponse updated = dogService.update(id, request, ETags.expectedVersions(ifMatch));
        return ETags.ok(updated, updated.version());
    }

    @Delete("/{id}")
    @Status(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a dog",
            description = "Marks the record as deleted. Nothing is removed from the database.")
    @ApiResponse(responseCode = "204", description = "The dog is now marked as deleted")
    @ApiResponse(responseCode = "404", description = "No dog has this identifier")
    public void delete(@PathVariable Long id) {
        dogService.delete(id);
    }
}
