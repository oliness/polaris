package uk.police.k9.dogs.controller;

import io.micronaut.core.annotation.Nullable;
import io.micronaut.data.model.Pageable;
import io.micronaut.http.HttpHeaders;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Delete;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.Put;
import io.micronaut.http.annotation.QueryValue;
import io.micronaut.http.annotation.Status;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import uk.police.k9.dogs.dto.PagedResponse;
import uk.police.k9.dogs.dto.ReferenceDataRequest;
import uk.police.k9.dogs.dto.ReferenceDataResponse;
import uk.police.k9.dogs.service.ReferenceDataService;

import java.net.URI;

/**
 * The endpoints every maintainable lookup exposes. Dog statuses and leaving reasons are the same
 * resource over different rows, so the routes are declared once and each subclass supplies only
 * its service and path; the subclass carries {@code @Controller}, which binds them to that path.
 */
public abstract class ReferenceDataController {

    private final ReferenceDataService<?> service;
    private final String basePath;

    protected ReferenceDataController(ReferenceDataService<?> service, String basePath) {
        this.service = service;
        this.basePath = basePath;
    }

    @Get
    @Operation(summary = "List the values",
            description = "Retired values are excluded unless includeDeleted is true.",
            parameters = {
                    @Parameter(name = "page", in = ParameterIn.QUERY, description = ApiParameters.PAGE,
                            schema = @Schema(implementation = Integer.class, defaultValue = "0")),
                    @Parameter(name = "size", in = ParameterIn.QUERY, description = ApiParameters.SIZE,
                            schema = @Schema(implementation = Integer.class, defaultValue = "20")),
                    @Parameter(name = "sort", in = ParameterIn.QUERY, description = ApiParameters.SORT,
                            example = "label,desc",
                            schema = @Schema(implementation = String.class))})
    @ApiResponse(responseCode = "200", description = "A page of values")
    @ApiResponse(responseCode = "400", description = "The paging parameters were invalid")
    public PagedResponse<ReferenceDataResponse> list(
            @Parameter(description = "Include values kept only for audit")
            @QueryValue(defaultValue = "false") boolean includeDeleted,
            @Parameter(hidden = true) Pageable pageable) {
        return service.list(includeDeleted, pageable);
    }

    @Get("/{id}")
    @Operation(summary = "Get a value")
    @ApiResponse(responseCode = "200", description = "The value",
            headers = @Header(name = "ETag", description = ApiParameters.ETAG,
                    schema = @Schema(implementation = String.class)))
    @ApiResponse(responseCode = "404", description = "Nothing has this identifier")
    public HttpResponse<ReferenceDataResponse> get(@PathVariable Long id) {
        ReferenceDataResponse value = service.get(id);
        return ETags.ok(value, value.version());
    }

    @Post
    @Operation(summary = "Add a value")
    @ApiResponse(responseCode = "201", description = "The value was added",
            headers = @Header(name = "ETag", description = ApiParameters.ETAG,
                    schema = @Schema(implementation = String.class)))
    @ApiResponse(responseCode = "400", description = "The request failed validation")
    @ApiResponse(responseCode = "409", description = "An active value already uses this code")
    public HttpResponse<ReferenceDataResponse> create(@Valid @Body ReferenceDataRequest request) {
        ReferenceDataResponse created = service.create(request);
        return ETags.created(created, URI.create(basePath + "/" + created.id()), created.version());
    }

    @Put("/{id}")
    @Operation(summary = "Replace a value")
    @ApiResponse(responseCode = "200", description = "The updated value",
            headers = @Header(name = "ETag", description = ApiParameters.ETAG,
                    schema = @Schema(implementation = String.class)))
    @ApiResponse(responseCode = "400", description = "The request failed validation")
    @ApiResponse(responseCode = "404", description = "Nothing has this identifier")
    @ApiResponse(responseCode = "409", description = "The value is deleted, or the code is taken")
    @ApiResponse(responseCode = "412", description = "The value has changed since If-Match was read")
    public HttpResponse<ReferenceDataResponse> update(
            @PathVariable Long id,
            @Valid @Body ReferenceDataRequest request,
            @Parameter(description = ApiParameters.IF_MATCH)
            @io.micronaut.http.annotation.Header(HttpHeaders.IF_MATCH) @Nullable String ifMatch) {
        ReferenceDataResponse updated = service.update(id, request, ETags.expectedVersions(ifMatch));
        return ETags.ok(updated, updated.version());
    }

    @Delete("/{id}")
    @Status(HttpStatus.NO_CONTENT)
    @Operation(summary = "Retire a value",
            description = "Marks the value as deleted. Dogs that already hold it keep it, but it can "
                    + "no longer be assigned.")
    @ApiResponse(responseCode = "204", description = "The value is now marked as deleted")
    @ApiResponse(responseCode = "404", description = "Nothing has this identifier")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
