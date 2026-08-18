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
import uk.police.k9.dogs.dto.PagedResponse;
import uk.police.k9.dogs.dto.SupplierRequest;
import uk.police.k9.dogs.dto.SupplierResponse;
import uk.police.k9.dogs.service.SupplierService;

import java.net.URI;

/**
 * The breeders and kennels the force takes dogs from. A supplier is a record in its own right
 * because more than one dog can come from the same place, and its details are corrected once.
 */
@Controller(ApiPaths.SUPPLIERS)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@ExecuteOn(TaskExecutors.BLOCKING)
@Validated
@Tag(name = "Suppliers")
public class SupplierController {

    private final SupplierService supplierService;

    public SupplierController(SupplierService supplierService) {
        this.supplierService = supplierService;
    }

    @Get
    @Operation(summary = "List suppliers",
            description = "Deleted suppliers are excluded unless includeDeleted is true.",
            parameters = {
                    @Parameter(name = "page", in = ParameterIn.QUERY, description = ApiParameters.PAGE,
                            schema = @Schema(implementation = Integer.class, defaultValue = "0")),
                    @Parameter(name = "size", in = ParameterIn.QUERY, description = ApiParameters.SIZE,
                            schema = @Schema(implementation = Integer.class, defaultValue = "20")),
                    @Parameter(name = "sort", in = ParameterIn.QUERY, description = ApiParameters.SORT,
                            example = "name,desc",
                            schema = @Schema(implementation = String.class))})
    @ApiResponse(responseCode = "200", description = "A page of suppliers")
    @ApiResponse(responseCode = "400", description = "The paging parameters were invalid")
    public PagedResponse<SupplierResponse> list(
            @Parameter(description = "Include suppliers kept only for audit")
            @QueryValue(defaultValue = "false") boolean includeDeleted,
            @Parameter(hidden = true) Pageable pageable) {
        return supplierService.list(includeDeleted, pageable);
    }

    @Get("/{id}")
    @Operation(summary = "Get a supplier")
    @ApiResponse(responseCode = "200", description = "The supplier",
            headers = @Header(name = "ETag", description = ApiParameters.ETAG,
                    schema = @Schema(implementation = String.class)))
    @ApiResponse(responseCode = "404", description = "No supplier has this identifier")
    public HttpResponse<SupplierResponse> get(@PathVariable Long id) {
        SupplierResponse supplier = supplierService.get(id);
        return ETags.ok(supplier, supplier.version());
    }

    @Post
    @Operation(summary = "Add a supplier")
    @ApiResponse(responseCode = "201", description = "The supplier was added",
            headers = @Header(name = "ETag", description = ApiParameters.ETAG,
                    schema = @Schema(implementation = String.class)))
    @ApiResponse(responseCode = "400", description = "The request failed validation")
    @ApiResponse(responseCode = "409", description = "An active supplier already uses this name")
    public HttpResponse<SupplierResponse> create(@Valid @Body SupplierRequest request) {
        SupplierResponse created = supplierService.create(request);
        return ETags.created(created, URI.create(ApiPaths.SUPPLIERS + "/" + created.id()),
                created.version());
    }

    @Put("/{id}")
    @Operation(summary = "Replace a supplier")
    @ApiResponse(responseCode = "200", description = "The updated supplier",
            headers = @Header(name = "ETag", description = ApiParameters.ETAG,
                    schema = @Schema(implementation = String.class)))
    @ApiResponse(responseCode = "400", description = "The request failed validation")
    @ApiResponse(responseCode = "404", description = "No supplier has this identifier")
    @ApiResponse(responseCode = "409", description = "The supplier is deleted, or the name is taken")
    @ApiResponse(responseCode = "412",
            description = "The supplier has changed since If-Match was read")
    public HttpResponse<SupplierResponse> update(
            @PathVariable Long id,
            @Valid @Body SupplierRequest request,
            @Parameter(description = ApiParameters.IF_MATCH)
            @io.micronaut.http.annotation.Header(HttpHeaders.IF_MATCH) @Nullable String ifMatch) {
        SupplierResponse updated = supplierService.update(id, request, ETags.expectedVersions(ifMatch));
        return ETags.ok(updated, updated.version());
    }

    @Delete("/{id}")
    @Status(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a supplier",
            description = "Marks the supplier as deleted. Dogs already sourced from it keep pointing "
                    + "at it, but no new dog can be assigned to it.")
    @ApiResponse(responseCode = "204", description = "The supplier is now marked as deleted")
    @ApiResponse(responseCode = "404", description = "No supplier has this identifier")
    public void delete(@PathVariable Long id) {
        supplierService.delete(id);
    }
}
