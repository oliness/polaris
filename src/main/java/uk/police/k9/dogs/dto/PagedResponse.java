package uk.police.k9.dogs.dto;

import io.micronaut.data.model.Page;
import io.micronaut.serde.annotation.Serdeable;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.function.Function;

/**
 * A single page of results. Deliberately a project-owned type rather than Micronaut's
 * {@code Page}, so the JSON shape is part of this application's published contract and does not
 * change when the framework's does.
 */
@Serdeable
@Schema(name = "PagedResponse", description = "A single page of results")
public record PagedResponse<T>(
        List<T> content,
        @Schema(description = "Zero-based index of this page", example = "0") int page,
        @Schema(description = "Maximum number of results per page", example = "20") int size,
        @Schema(description = "Total results matching the query", example = "137") long totalElements,
        @Schema(description = "Total pages available", example = "7") int totalPages
) {

    /** Converts a repository page into an API page, mapping each element on the way. */
    public static <E, T> PagedResponse<T> from(Page<E> source, Function<E, T> mapper) {
        return new PagedResponse<>(
                source.getContent().stream().map(mapper).toList(),
                source.getPageNumber(),
                source.getSize(),
                source.getTotalSize(),
                source.getTotalPages()
        );
    }
}
