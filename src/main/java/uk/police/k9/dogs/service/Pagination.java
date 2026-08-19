package uk.police.k9.dogs.service;

import io.micronaut.data.model.Pageable;
import io.micronaut.data.model.Sort;

/** Applied to every paged query. */
final class Pagination {

    private Pagination() {
    }

    /**
     * Without a default sort the database may reorder rows between requests, so page 2 can repeat
     * or skip page 1. {@code withTotal()} is needed because a {@code Pageable} not asked for a
     * total refuses to report one.
     */
    static Pageable normalise(Pageable pageable, Sort defaultSort) {
        Pageable sorted = pageable.isSorted() ? pageable : pageable.withSort(defaultSort);
        return sorted.withTotal();
    }
}
